package spixie

import io.reactivex.subjects.BehaviorSubject
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.util.Duration
import org.apache.commons.lang3.math.Fraction
import spixie.static.initCustomPanning
import spixie.static.runInUIAndWait
import spixie.visualEditor.Component
import spixie.visualEditor.Module
import spixie.visualEditor.VisualEditor
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.roundToInt

class ArrangementWindow: BorderPane(), WorkingWindowOpenableContent {
    private val contentPane = Pane()
    private val content = Group()
    val graphBuilderGroup = Group()
    private val grid = Group()
    val graphCanvases = Group()
    val visualEditor = VisualEditor()
    private val cursor = Line(-0.5, 0.0, -0.5, 10000.0)
    private val zoom = BehaviorSubject.createDefault(Fraction.getFraction(64.0))
    private val selectionBlock = ArrangementSelectionBlock(zoom)
    private val timePointer = Line(-0.5, 0.0, -0.5, 10000.0).apply {
        strokeWidth = 32.0
        startXProperty().addListener { _, _, newValue ->
            stroke = LinearGradient(
                    newValue.toDouble()-16.0,
                    0.0,
                    newValue.toDouble()+16.0,
                    0.0,
                    false,
                    CycleMethod.NO_CYCLE,
                    Stop(0.0, javafx.scene.paint.Color(1.0, 0.0, 0.0, 0.0)),
                    Stop(0.47, javafx.scene.paint.Color(1.0, 0.0, 0.0, 0.3)),
                    Stop(0.50, javafx.scene.paint.Color(0.0, 0.0, 0.0, 1.0)),
                    Stop(0.53, javafx.scene.paint.Color(1.0, 0.0, 0.0, 0.3)),
                    Stop(1.0, javafx.scene.paint.Color(1.0, 0.0, 0.0, 0.0))
            )
        }

    }
    var timePointerCentering = false
        set(value) {
            field = value
            if(value){
                updateTimePointer()
            }
        }
    private val waveform = Canvas(1.0, 300.0)
    private val graphsTree = Pane()
    val graphs = arrayListOf<ArrangementGraphsContainer>()

    private fun redrawWaveform() {
        val newWaveformLayoutX = -content.layoutX - 100
        val startTime = (-content.layoutX - 100)*64/100.0/zoom.value!!.toDouble() - Main.renderManager.offset.value
        val endTime = (-content.layoutX + width + 100)*64/100.0/zoom.value!!.toDouble() - Main.renderManager.offset.value

        val startSecond = startTime*3600/Main.renderManager.bpm.value/60
        val endSecond = endTime*3600/Main.renderManager.bpm.value/60

        val secondsInPixel = (endSecond - startSecond) / waveform.width


        val bufferedImage = BufferedImage(waveform.width.toInt(), waveform.height.toInt(), BufferedImage.TYPE_4BYTE_ABGR)
        val g = bufferedImage.createGraphics()
        g.color = Color(66, 170, 255, 255)
        val rms = Main.audio.rms
        if(rms.isNotEmpty()){
            var from = 0
            for(x in 0 until bufferedImage.width){
                var maxrms = 0.0f
                val to = Math.round((startSecond + x * secondsInPixel)*100).toInt()
                if(from > to){
                    from = to
                }
                for(i in from..to){
                    if(i>0 && i<rms.size){
                        val r = rms[i]
                        if(r>maxrms) maxrms = r
                    }
                }
                from = to
                g.drawLine(x, ((0.5+ maxrms /2)*waveform.height).toInt(), x, ((0.5- maxrms /2)*waveform.height).toInt())
            }
        }
        val toFXImage = SwingFXUtils.toFXImage(bufferedImage, null)
        waveform.graphicsContext2D.clearRect(0.0, 0.0, waveform.width, waveform.height)
        waveform.graphicsContext2D.drawImage(toFXImage, 0.0, 0.0)
        waveform.layoutX = newWaveformLayoutX
    }

    private fun updateGrid(){
        contentPane.style = "-fx-background-color: #FFFFFFFF, linear-gradient(from ${content.layoutX+0.5}px 0px to ${content.layoutX+200.5}px 0px, repeat, #00000066 0.26%, transparent 0.26%), linear-gradient(from ${content.layoutX+100.5}px 0px to ${content.layoutX+300.5}px 0px, repeat, #00000019 0.26%, transparent 0.26%),linear-gradient(from ${content.layoutX+200.5}px 0px to ${content.layoutX+400.5}px 0px, repeat, #00000019 0.26%, transparent 0.26%),linear-gradient(from ${content.layoutX+300.5}px 0px to ${content.layoutX+500.5}px 0px, repeat, #00000019 0.26%, transparent 0.26%),linear-gradient(from 0px ${content.layoutY-49.5}px to 0px ${content.layoutY+50.5}px, repeat, #00000010 50.5%, transparent 50.5%);"
    }

    var needRedrawAllGraphs = false
    private var needUpdateGrid = false
    var needRedrawWaveform = false

    fun perFrame(){
        if(needRedrawAllGraphs){
            redrawGraph(null)
            needRedrawAllGraphs = false
        }
        if(needUpdateGrid){
            updateGrid()
            needUpdateGrid = false
        }
        if(needRedrawWaveform){
            redrawWaveform()
            needRedrawWaveform = false
        }
    }

    fun redrawGraph(only: ArrangementGraph?){
        val newGraphsLayoutX = -content.layoutX - 100
        val startTime = (-content.layoutX - 100)*64/100.0/zoom.value!!.toDouble()
        val endTime = (-content.layoutX + width + 100)*64/100.0/zoom.value!!.toDouble()
        val redraw = { graph: ArrangementGraph ->
            val canvas = graph.canvas
            canvas.width = width + 200.0
            canvas.layoutX = newGraphsLayoutX
            val g = canvas.graphicsContext2D
            g.clearRect(0.0, 0.0, canvas.width, canvas.height)
            val beatsInPixel = (endTime - startTime) / canvas.width
            g.lineWidth = 1.0

            g.stroke = javafx.scene.paint.Color(1.0, 0.0, 0.0, 1.0)
            var x = 0
            while (x < canvas.width) {
                val y1 = 1.0 - graph.data.getValue(startTime + x * beatsInPixel)
                val y2 = 1.0 - graph.data.getValue(startTime + (x + 1.0) * beatsInPixel)
                g.strokeLine(x.toDouble(), y1 * canvas.height, (x + 1).toDouble(), y2 * canvas.height)
                x++
            }
        }
        if (only != null) {
            runInUIAndWait {
                redraw(only)
            }
        } else {
            runInUIAndWait {
                graphs.forEach {
                    if(it.expanded){
                        it.list.forEach {
                            redraw(it)
                        }
                    }
                }
            }
        }
    }

    fun updateGraphTree(){
        graphCanvases.children.clear()
        val graphsTreeContent = graphsTree.children[0] as Pane
        graphsTreeContent.children.clear()
        layout()
        var i = 3
        graphs.forEach { graphContainter->
            graphsTreeContent.children.add(Pane().apply {
                style="-fx-background-color: #FFFFFFDD;";
                setMinSize(graphsTree.width, 100.0)
                layoutY = i*100.0
                children.add(graphContainter.name)
                setOnMouseClicked { event->
                    if(event.button == MouseButton.PRIMARY){
                        graphContainter.expanded = !graphContainter.expanded
                        updateGraphTree()
                    }
                }
                setOnContextMenuRequested { event->
                    ContextMenu(
                            MenuItem("New subgraph").apply {
                                setOnAction {
                                    graphContainter.list.add(ArrangementGraph())
                                    graphContainter.expanded=true
                                    updateGraphTree()
                                }
                            },
                            MenuItem("Delete graph").apply {
                                setOnAction {
                                    graphs.remove(graphContainter)
                                    graphContainter.name.value="DELETED"
                                    updateGraphTree()
                                }
                            }
                    ).show(this, event.screenX, event.screenY)
                    event.consume()
                }
            })
            i++

            if(graphContainter.expanded){
                graphContainter.list.forEach { graph->
                    graphsTreeContent.children.add(Pane().apply {
                        style="-fx-background-color: #FFFFFFCC;";
                        setMinSize(graphsTree.width, 100.0)
                        layoutY = i*100.0
                        children.add(
                                VBox(
                                        graph.rangeFromControl,graph.rangeToControl
                                )
                        )
                        setOnContextMenuRequested { event->
                            ContextMenu(
                                    MenuItem("Delete subgraph").apply {
                                        setOnAction {
                                            graphContainter.list.remove(graph)
                                            updateGraphTree()
                                        }
                                    }
                            ).show(this, event.screenX, event.screenY)
                            event.consume()
                        }
                    })
                    graphCanvases.children.add(graph.canvas)
                    graph.canvas.layoutY = i*100.0
                    i++
                }
            }
        }
        needRedrawAllGraphs = true
    }

    init {
        widthProperty().addListener { _, _, newValue ->
            waveform.width = newValue.toDouble() + 200.0
            needRedrawWaveform = true
            needRedrawAllGraphs = true
        }

        contentPane.initCustomPanning(content, false)

        Main.renderManager.time.timeChanges.subscribe {
            updateTimePointer()
        }

        setOnMouseMoved { event ->
            updateCursor(event)
        }

        contentPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, { event ->
            updateCursor(event)
            if(event.isControlDown){
                val screenToLocal = content.screenToLocal(event.screenX, event.screenY)
                Main.renderManager.time.time = screenToLocal.x*64/100.0/zoom.value!!.toDouble()
            }
        })

        var mousePressedCursorX:Double? = null
        var mousePressedLine:Int? = null
        contentPane.addEventHandler(MouseEvent.MOUSE_PRESSED, { event ->
            if(event.isConsumed) return@addEventHandler
            if(event.button == MouseButton.PRIMARY){
                mousePressedCursorX = cursor.startX
                mousePressedLine = (content.screenToLocal(event.screenX, event.screenY).y/100.0).toInt()
            }
            requestFocus()
        })

        contentPane.addEventHandler(MouseEvent.MOUSE_RELEASED, { event ->
            if(event.button == MouseButton.PRIMARY){
                mousePressedCursorX?.let { pressedCursorX ->
                    mousePressedLine?.let { pressedLine ->
                        if(pressedCursorX == cursor.startX){
                            val restartAudio = Main.audio.isPlaying()
                            if(restartAudio){
                                Main.audio.pause()
                            }
                            Main.renderManager.time.time = (cursor.startX-0.5)*64/100.0/zoom.value!!.toDouble()
                            if(restartAudio){
                                Main.audio.play(Duration.seconds(Math.round((Main.renderManager.time.time-Main.renderManager.offset.value)*3600/Main.renderManager.bpm.value)/60.0))
                            }
                        }
                        val time1 = (cursor.startX-0.5)*64/100.0/zoom.value!!.toDouble()
                        val time2 = (pressedCursorX-0.5)*64/100.0/zoom.value!!.toDouble()
                        if(time1 < time2){
                            selectionBlock.timeStart = Fraction.getFraction(time1)
                            selectionBlock.timeEnd = Fraction.getFraction(time2)
                        }else{
                            selectionBlock.timeStart = Fraction.getFraction(time2)
                            selectionBlock.timeEnd = Fraction.getFraction(time1)
                        }

                        selectionBlock.line = pressedLine
                        selectionBlock.graph = graphs.mapNotNull {
                            it.list.find { it.canvas.layoutY==selectionBlock.layoutY }
                        }.firstOrNull()
                        mousePressedCursorX = null
                        mousePressedLine = null
                    }
                }
            }
            requestFocus()
        })

        setOnScroll { event ->
            if(event.deltaY<0){
                if(zoom.value!!.compareTo(Fraction.ONE) != 0){
                    val cursorTime = (cursor.startX-0.5)*64/100.0/zoom.value!!.toDouble()
                    zoom.onNext(zoom.value!!.divideBy(Fraction.getFraction(2.0)))
                    var cursorNewX = cursorTime*zoom.value!!.toDouble()*100.0/64 + 0.5
                    content.layoutX += (cursor.startX - cursorNewX).roundToInt()
                    if(content.layoutX > 0) {
                        cursorNewX += content.layoutX
                        content.layoutX = 0.0
                    }
                    cursor.startX = cursorNewX
                    cursor.endX = cursorNewX

                    needRedrawWaveform = true
                    needRedrawAllGraphs = true
                    updateTimePointer()

                }
            }
            if(event.deltaY>0){
                if(zoom.value!!.compareTo(Fraction.getFraction(4096.0)) != 0){
                    val cursorTime = (cursor.startX-0.5)*64/100.0/zoom.value!!.toDouble()
                    zoom.onNext(zoom.value!!.multiplyBy(Fraction.getFraction(2.0)))
                    var cursorNewX = cursorTime*zoom.value!!.toDouble()*100.0/64 + 0.5
                    content.layoutX += (cursor.startX - cursorNewX).roundToInt()
                    if(content.layoutX > 0) {
                        cursorNewX += content.layoutX
                        content.layoutX = 0.0
                    }
                    cursor.startX = cursorNewX
                    cursor.endX = cursorNewX

                    needRedrawWaveform = true
                    needRedrawAllGraphs = true
                    updateTimePointer()
                }
            }
            event.consume()
        }

        setOnKeyPressed { event ->
            if(event.isControlDown && !event.isAltDown && !event.isShiftDown){
                if(event.code == KeyCode.C){
                    selectionBlock.copy()
                }
                if(event.code == KeyCode.V){
                    selectionBlock.paste()
                }
                if(event.code == KeyCode.D){
                    selectionBlock.duplicate()
                }
                if(event.code == KeyCode.R){
                    selectionBlock.reverse()
                }
            }
            if(!event.isControlDown && !event.isAltDown && !event.isShiftDown){
                if(event.code == KeyCode.C){
                    timePointerCentering = true
                }
                if(event.code == KeyCode.V){
                    Main.workingWindow.open(visualEditor)
                }
                if(event.code == KeyCode.Q){
                    selectionBlock.buildGraph()
                }
                if(event.code == KeyCode.E){
                    selectionBlock.editGraph()
                }
                if(event.code == KeyCode.DELETE){
                    selectionBlock.del()
                }
            }
        }

        setOnKeyReleased { event ->
            if(event.code == KeyCode.C){
                timePointerCentering = false
            }
        }

        needUpdateGrid = true

        content.children.addAll(waveform, grid, graphCanvases, selectionBlock, cursor, timePointer, graphBuilderGroup)
        center = contentPane.apply { children.add(content) }

        left = graphsTree.apply {
            minWidth = 100.0
            maxWidth = 100.0
            children.addAll(Pane())

            setOnContextMenuRequested { event->
                ContextMenu(
                        MenuItem("New Graph").apply {
                            setOnAction {
                                graphs.add(ArrangementGraphsContainer().apply { name.value="Graph ${graphs.size+1}" })
                                updateGraphTree()
                            }
                        }
                ).show(graphsTree, event.screenX, event.screenY)
            }
        }

        updateGraphTree()

        contentPane.clip = Rectangle(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        graphsTree.clip = Rectangle(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        content.layoutXProperty().addListener { _, _, _ ->
            needRedrawWaveform = true
            needUpdateGrid = true
            needRedrawAllGraphs = true
        }
        content.layoutYProperty().addListener { _, _, y ->
            waveform.layoutY = -y.toDouble()
            graphsTree.children[0].layoutY = y.toDouble()
            needUpdateGrid = true
            needRedrawAllGraphs = true
        }
    }

    fun save(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        objectOutputStream.writeDouble(Main.renderManager.bpm.value)
        objectOutputStream.writeDouble(Main.renderManager.offset.value)

        val modules = visualEditor.modules.map { it.toSerializable() }
        objectOutputStream.writeObject(modules)

        objectOutputStream.writeObject(graphs)

        objectOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun load(objectInputStream: ObjectInputStream){
        try{
            Main.renderManager.bpm.value = objectInputStream.readDouble()
            Main.renderManager.offset.value = objectInputStream.readDouble()
            visualEditor.modules.clear()
            val modules = objectInputStream.readObject() as List<Triple<String, List<Component>, List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>>
            modules.forEach {
                val module = Module(it.first).apply {
                    fromSerializable(it)
                    reconnectPins()
                }
                visualEditor.modules.add(module)
            }
            visualEditor.modules.forEach { it.updateModuleComponents() }

            visualEditor.modules.find { it.isMain }?.let {
                visualEditor.loadModule(it)
            }

            graphs.clear()
            graphs.addAll(objectInputStream.readObject() as ArrayList<ArrangementGraphsContainer>)
            updateGraphTree()
            needRedrawAllGraphs = true
            Main.renderManager.clearCache()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun getSelectedFrames(): Pair<Int, Int>{
        return (selectionBlock.timeStart.toDouble()*3600/Main.renderManager.bpm.value).toInt() to (selectionBlock.timeEnd.toDouble()*3600/Main.renderManager.bpm.value).toInt()
    }

    private fun updateCursor(event:MouseEvent){
        val sceneToLocal = content.sceneToLocal(event.sceneX, event.sceneY)
        val x = Math.round(sceneToLocal.x/6.25)*6.25 + 0.5
        cursor.startX = x
        cursor.endX = x
    }

    private fun updateTimePointer(){
        val x = Math.round(zoom.value!!.toDouble() / 64.0 * Main.renderManager.time.time * 100.0).toDouble()
        timePointer.startX = x
        timePointer.endX = x
        if(timePointerCentering){
            content.layoutX = (-(timePointer.startX - width/5)).coerceAtMost(0.0)
        }
    }
}
