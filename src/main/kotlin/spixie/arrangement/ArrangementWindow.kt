package spixie.arrangement

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
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
import spixie.Core
import spixie.WorkWindow
import spixie.static.initCustomPanning
import spixie.static.runInUIAndWait
import spixie.visualEditor.Module
import spixie.visualEditor.VisualEditor
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

class ArrangementWindow: BorderPane(), WorkWindow.OpenableContent {
    private val contentPane = Pane()

    private val content = Group()

    val graphBuilderGroup = Group()

    private val grid = Group()

    private val graphCanvases = Group()

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

    val spectrogram = Spectrogram(content)

    private val graphsTree = Pane().apply {
        style="-fx-background-color: #101B2CFF"
    }

    val graphs = arrayListOf<ArrangementGraphsContainer>()

    private fun updateGrid(){
        contentPane.style = "-fx-background-color: #FFFFFFFF, linear-gradient(from ${content.layoutX+0.5}px 0px to ${content.layoutX+200.5}px 0px, repeat, #00000066 0.26%, transparent 0.26%), linear-gradient(from ${content.layoutX+100.5}px 0px to ${content.layoutX+300.5}px 0px, repeat, #00000019 0.26%, transparent 0.26%),linear-gradient(from ${content.layoutX+200.5}px 0px to ${content.layoutX+400.5}px 0px, repeat, #00000019 0.26%, transparent 0.26%),linear-gradient(from ${content.layoutX+300.5}px 0px to ${content.layoutX+500.5}px 0px, repeat, #00000019 0.26%, transparent 0.26%),linear-gradient(from 0px ${content.layoutY-49.5}px to 0px ${content.layoutY+50.5}px, repeat, #00000010 50.5%, transparent 50.5%);"
    }

    var needRedrawAllGraphs = false

    private var needUpdateGrid = false

    fun perFrame(){
        if(needRedrawAllGraphs){
            redrawGraph(null)
            needRedrawAllGraphs = false
        }
        if(needUpdateGrid){
            updateGrid()
            needUpdateGrid = false
        }
    }

    fun redrawGraph(only: ArrangementGraph?){
        val newGraphsLayoutX = -content.layoutX - 100
        val startTime = calcTimeOfX(-content.layoutX - 100)
        val endTime = calcTimeOfX(-content.layoutX + width + 100)
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

    private fun updateGraphTree(){
        graphCanvases.children.clear()
        val graphsTreeContent = graphsTree.children[0] as Pane
        graphsTreeContent.children.clear()
        layout()
        var i = 3
        graphs.forEach { graphContainter->
            graphsTreeContent.children.add(Pane().apply {
                style="-fx-background-color: #FFFFFFFF;"
                setMinSize(graphsTree.width-1, 100.0)
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
                        style="-fx-background-color: #FFFFFFFF;"
                        setMinSize(graphsTree.width-1, 100.0)
                        layoutY = i*100.0
                        children.add(
                                VBox(
                                        graph.rangeMaxControl, graph.rangeMinControl
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
            spectrogram.width = newValue.toDouble() + 200.0
            spectrogram.requestRedraw()
            needRedrawAllGraphs = true
        }

        contentPane.initCustomPanning(content, false)

        Core.renderManager.time.timeChanges.subscribe {
            updateTimePointer()
        }

        setOnMouseMoved { event ->
            updateCursor(event)
        }

        contentPane.addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
            updateCursor(event)
            if(event.isControlDown){
                val screenToLocal = content.screenToLocal(event.screenX, event.screenY)
                Core.renderManager.time.time = calcTimeOfX(screenToLocal.x)
            }
        }

        var mousePressedCursorX:Double? = null
        var mousePressedLine:Int? = null
        contentPane.addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
            if(event.isConsumed) return@addEventHandler
            if(event.button == MouseButton.PRIMARY){
                mousePressedCursorX = cursor.startX
                mousePressedLine = (content.screenToLocal(event.screenX, event.screenY).y/100.0).toInt()
            }
            requestFocus()
        }

        contentPane.addEventHandler(MouseEvent.MOUSE_RELEASED) { event ->
            if(event.button == MouseButton.PRIMARY){
                mousePressedCursorX?.let { pressedCursorX ->
                    mousePressedLine?.let { pressedLine ->
                        if(pressedCursorX == cursor.startX){
                            val restartAudio = Core.audio.isPlaying()
                            if(restartAudio){
                                Core.audio.pause()
                            }
                            Core.renderManager.time.time = calcTimeOfX(cursor.startX-0.5)
                            if(restartAudio){
                                Core.audio.play(Duration.seconds(Math.round((Core.renderManager.time.time- Core.renderManager.offset.value)*3600/ Core.renderManager.bpm.value)/60.0))
                            }
                        }
                        val time1 = calcTimeOfX(cursor.startX-0.5)
                        val time2 = calcTimeOfX(pressedCursorX-0.5)
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
        }

        setOnScroll { event ->
            if(event.isControlDown){
                if(event.deltaY<0){
                    if(zoom.value!!.compareTo(Fraction.ONE) != 0){
                        val cursorTime = calcTimeOfX(cursor.startX-0.5)
                        zoom.onNext(zoom.value!!.divideBy(Fraction.getFraction(2.0)))
                        var cursorNewX = cursorTime*zoom.value!!.toDouble()*100.0/64 + 0.5
                        content.layoutX += (cursor.startX - cursorNewX).roundToInt()
                        if(content.layoutX > 0) {
                            cursorNewX += content.layoutX
                            content.layoutX = 0.0
                        }
                        cursor.startX = cursorNewX
                        cursor.endX = cursorNewX

                        spectrogram.requestRedraw()
                        needRedrawAllGraphs = true
                        updateTimePointer()

                    }
                }
                if(event.deltaY>0){
                    if(zoom.value!!.compareTo(Fraction.getFraction(4096.0)) != 0){
                        val cursorTime = calcTimeOfX(cursor.startX-0.5)
                        zoom.onNext(zoom.value!!.multiplyBy(Fraction.getFraction(2.0)))
                        var cursorNewX = cursorTime*zoom.value!!.toDouble()*100.0/64 + 0.5
                        content.layoutX += (cursor.startX - cursorNewX).roundToInt()
                        if(content.layoutX > 0) {
                            cursorNewX += content.layoutX
                            content.layoutX = 0.0
                        }
                        cursor.startX = cursorNewX
                        cursor.endX = cursorNewX

                        spectrogram.requestRedraw()
                        needRedrawAllGraphs = true
                        updateTimePointer()
                    }
                }
                event.consume()
            }
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
                    Core.workWindow.open(visualEditor)
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

        content.children.addAll(grid, graphCanvases, spectrogram, selectionBlock, cursor, timePointer, graphBuilderGroup)
        center = contentPane.apply { children.add(content) }

        left = graphsTree.apply {
            minWidth = 100.0
            maxWidth = 100.0
            children.addAll(Pane())
            clip = Rectangle(0.0, 0.0, minWidth - 1.0, Double.POSITIVE_INFINITY)

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

        content.layoutXProperty().addListener { _, _, _ ->
            spectrogram.requestRedraw()
            needUpdateGrid = true
            needRedrawAllGraphs = true
        }
        content.layoutYProperty().addListener { _, _, y ->
            spectrogram.layoutY = -y.toDouble()
            graphsTree.children[0].layoutY = y.toDouble()
            needUpdateGrid = true
            needRedrawAllGraphs = true
        }
    }

    fun calcTimeOfX(x: Double): Double {
        return x*64/100.0/zoom.value!!.toDouble()
    }

    fun serialize(): ByteArray {
        val serializedProject = SerializedProject().apply {
            bpm = Core.renderManager.bpm.value
            offset = Core.renderManager.offset.value
            module = visualEditor.mainModule.serialize()
            this.graphs = this@ArrangementWindow.graphs
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        val output = Output(byteArrayOutputStream)
        Core.kryo.writeObject(output, serializedProject)
        output.flush()
        output.close()

        return byteArrayOutputStream.toByteArray()
    }

    fun deserializeAndLoad(inputStream: InputStream){
        try{
            val serializedProject = Core.kryo.readObject(Input(inputStream), SerializedProject::class.java)

            Core.renderManager.bpm.value = serializedProject.bpm
            Core.renderManager.offset.value = serializedProject.offset
            visualEditor.modules.clear()

            visualEditor.loadModule(
                    Module().apply {
                        deserizalize(serializedProject.module)
                        reconnectPins()
                    }
            )

            graphs.clear()
            graphs.addAll(serializedProject.graphs)
            updateGraphTree()
            needRedrawAllGraphs = true
            Core.renderManager.clearCache()
        }catch (e:Exception){
            e.printStackTrace()
            Platform.exit()
        }
    }

    fun getSelectedFrames(): Pair<Int, Int>{
        return (selectionBlock.timeStart.toDouble()*3600/ Core.renderManager.bpm.value).toInt() to (selectionBlock.timeEnd.toDouble()*3600/ Core.renderManager.bpm.value).toInt()
    }

    private fun updateCursor(event:MouseEvent){
        val sceneToLocal = content.sceneToLocal(event.sceneX, event.sceneY)
        val x = Math.round(sceneToLocal.x/6.25)*6.25 + 0.5
        cursor.startX = x
        cursor.endX = x
    }

    private fun updateTimePointer(){
        val x = Math.round(zoom.value!!.toDouble() / 64.0 * Core.renderManager.time.time * 100.0).toDouble()
        timePointer.startX = x
        timePointer.endX = x
        if(timePointerCentering){
            content.layoutX = (-(timePointer.startX - width/5)).coerceAtMost(0.0)
        }
    }
}
