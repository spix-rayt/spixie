package spixie

import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.util.Duration
import org.apache.commons.math3.fraction.Fraction
import spixie.visual_editor.VisualEditor
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

class ArrangementWindow: Pane(), WorkingWindowOpenableContent {
    val content = Group()
    val graphBuilderGroup = Group()
    val grid = Group()
    val graphCanvases = Group()
    val visualEditor = VisualEditor()
    val cursor = Line(-0.5, 0.0, -0.5, 10000.0)
    val zoom = FractionImmutablePointer(Fraction(64))
    val selectionBlock = ArrangementSelectionBlock(zoom)
    val timePointer = Line(-0.5, 0.0, -0.5, 10000.0)
    var timePointerCentering = false
        set(value) {
            field = value
            if(value){
                updateTimePointer()
            }
        }
    val waveform = Canvas(1.0, 300.0)
    val graphs = HashMap<Int, ArrangementGraph>()

    fun redrawWaveform() {
        val newWaveformLayoutX = -content.layoutX - 100
        val startTime = (-content.layoutX - 100)*64/100.0/zoom.value.toDouble()
        val endTime = (-content.layoutX + width + 100)*64/100.0/zoom.value.toDouble()

        val startSecond = startTime*3600/Main.renderManager.bpm.value.value/60
        val endSecond = endTime*3600/Main.renderManager.bpm.value.value/60

        val secondsInPixel = (endSecond - startSecond) / waveform.width


        val bufferedImage = BufferedImage(waveform.width.toInt(), waveform.height.toInt(), BufferedImage.TYPE_4BYTE_ABGR)
        val g = bufferedImage.createGraphics()
        g.color = Color(66, 170, 255, 255)
        val rms = Main.audio.rms
        if(rms.isNotEmpty()){
            var from = 0
            for(x in 0 until bufferedImage.width){
                var maxrms = 0.0f
                var to = Math.round((startSecond + x * secondsInPixel)*100).toInt()
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

    fun updateGrid(){
        style = "-fx-background-color: #FFFFFFFF, linear-gradient(from ${content.layoutX+0.5}px 0px to ${content.layoutX+200.5}px 0px, repeat, #00000066 0.25%, transparent 0.25%), linear-gradient(from ${content.layoutX+100.5}px 0px to ${content.layoutX+300.5}px 0px, repeat, #00000019 0.25%, transparent 0.25%),linear-gradient(from ${content.layoutX+200.5}px 0px to ${content.layoutX+400.5}px 0px, repeat, #00000019 0.25%, transparent 0.25%),linear-gradient(from ${content.layoutX+300.5}px 0px to ${content.layoutX+500.5}px 0px, repeat, #00000019 0.25%, transparent 0.25%),linear-gradient(from 0px ${content.layoutY+0.5}px to 0px ${content.layoutY+50.5}px, repeat, #00000019 1%, transparent 1%);"
    }

    var needUpdateAllGraphs = false
    var needUpdateGrid = false
    var needRedrawWaveform = false

    fun perFrame(){
        if(needUpdateAllGraphs){
            updateGraph(null)
            needUpdateAllGraphs = false
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

    fun updateGraph(only: ArrangementGraph?){
        val newGraphsLayoutX = -content.layoutX - 100
        val startTime = (-content.layoutX - 100)*64/100.0/zoom.value.toDouble()
        val endTime = (-content.layoutX + width + 100)*64/100.0/zoom.value.toDouble()
        val redraw = { graph: ArrangementGraph ->
            val canvas = graph.canvas
            canvas.layoutX = newGraphsLayoutX
            val g = canvas.graphicsContext2D
            g.clearRect(0.0, 0.0, canvas.width, canvas.height)
            val beatsInPixel = (endTime - startTime) / canvas.width
            g.lineWidth = 1.0

            g.stroke = javafx.scene.paint.Color(1.0, 0.0, 0.0, 1.0)
            var x = 0
            while (x < canvas.width) {
                val y1 = graph.data.getValue(startTime + x * beatsInPixel)
                val y2 = graph.data.getValue(startTime + (x + 1.0) * beatsInPixel)
                g.strokeLine(x.toDouble(), y1 * canvas.height, (x + 1).toDouble(), y2 * canvas.height)
                x++
            }
        }
        if (only != null) {
            redraw(only)
        } else {
            for (graph in graphs) {
                redraw(graph.value)
            }
        }
    }

    init {
        widthProperty().addListener { _, _, newValue ->
            waveform.width = newValue.toDouble() + 200.0
            for (graph in graphs) {
                graph.value.canvas.width = newValue.toDouble() + 200.0
            }
        }

        initCustomPanning()



        timePointer.strokeWidth = 4.0
        Main.renderManager.time.timeChanges.subscribe {
            updateTimePointer()
        }

        setOnMouseMoved { event ->
            updateCursor(event)
        }

        addEventFilter(MouseEvent.MOUSE_DRAGGED, { event ->
            updateCursor(event)
            if(event.isControlDown){
                val screenToLocal = content.screenToLocal(event.screenX, event.screenY)
                Main.renderManager.time.time = screenToLocal.x*64/100.0/zoom.value.toDouble()
            }
        })

        var mousePressedCursorX:Double? = null
        var mousePressedLine:Int? = null
        addEventHandler(MouseEvent.MOUSE_PRESSED, { event ->
            if(event.isConsumed) return@addEventHandler
            if(event.button == MouseButton.PRIMARY){
                mousePressedCursorX = cursor.startX
                mousePressedLine = (content.screenToLocal(event.screenX, event.screenY).y/100.0).toInt()
            }
        })

        addEventHandler(MouseEvent.MOUSE_RELEASED, { event ->
            if(event.button == MouseButton.PRIMARY){
                mousePressedCursorX?.let { pressedCursorX ->
                    mousePressedLine?.let { pressedLine ->
                        if(pressedCursorX == cursor.startX){
                            val restartAudio = Main.audio.isPlaying()
                            if(restartAudio){
                                Main.audio.pause()
                            }
                            Main.renderManager.time.time = (cursor.startX-0.5)*64/100.0/zoom.value.toDouble()
                            if(restartAudio){
                                Main.audio.play(Duration.seconds(Main.renderManager.time.frame/60.0))
                            }
                        }
                        val time1 = (cursor.startX-0.5)*64/100.0/zoom.value.toDouble()
                        val time2 = (pressedCursorX-0.5)*64/100.0/zoom.value.toDouble()
                        if(time1 < time2){
                            selectionBlock.timeStart = Fraction(time1)
                            selectionBlock.timeEnd = Fraction(time2)
                        }else{
                            selectionBlock.timeStart = Fraction(time2)
                            selectionBlock.timeEnd = Fraction(time1)
                        }

                        selectionBlock.line = pressedLine
                        mousePressedCursorX = null
                        mousePressedLine = null
                    }
                }
            }
        })

        setOnScroll { event ->
            if(event.deltaY<0){
                if(zoom.value.compareTo(Fraction.ONE) != 0){
                    val cursorTime = (cursor.startX-0.5)*64/100.0/zoom.value.toDouble()
                    zoom.value = zoom.value.divide(2)
                    var cursorNewX = cursorTime*zoom.value.toDouble()*100.0/64 + 0.5
                    content.layoutX += cursor.startX - cursorNewX
                    if(content.layoutX > 0) {
                        cursorNewX += content.layoutX
                        content.layoutX = 0.0
                    }
                    cursor.startX = cursorNewX
                    cursor.endX = cursorNewX

                    needRedrawWaveform = true
                    needUpdateAllGraphs = true
                    updateTimePointer()
                    selectionBlock.updateZoom()
                }
            }
            if(event.deltaY>0){
                if(zoom.value.compareTo(Fraction(4096)) != 0){
                    val cursorTime = (cursor.startX-0.5)*64/100.0/zoom.value.toDouble()
                    zoom.value = zoom.value.multiply(2)
                    var cursorNewX = cursorTime*zoom.value.toDouble()*100.0/64 + 0.5
                    content.layoutX += cursor.startX - cursorNewX
                    if(content.layoutX > 0) {
                        cursorNewX += content.layoutX
                        content.layoutX = 0.0
                    }
                    cursor.startX = cursorNewX
                    cursor.endX = cursorNewX

                    needRedrawWaveform = true
                    needUpdateAllGraphs = true
                    updateTimePointer()
                    selectionBlock.updateZoom()
                }
            }
            event.consume()
        }

        needUpdateGrid = true

        content.children.addAll(waveform, grid, graphCanvases, selectionBlock, cursor, timePointer, graphBuilderGroup)
        children.addAll(content)

        clip = Rectangle(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        content.layoutXProperty().addListener { _, _, _ ->
            needRedrawWaveform = true
            needUpdateGrid = true
            needUpdateAllGraphs = true
        }
        content.layoutYProperty().addListener { _, _, y ->
            waveform.layoutY = -y.toDouble()
            needUpdateGrid = true
            needUpdateAllGraphs = true
        }
    }

    private fun updateCursor(event:MouseEvent){
        val sceneToLocal = content.sceneToLocal(event.sceneX, event.sceneY)
        val x = Math.round(sceneToLocal.x/6.25)*6.25 + 0.5
        cursor.startX = x
        cursor.endX = x
    }

    private fun updateTimePointer(){
        val x = Math.round(zoom.value.toDouble() / 64.0 * Main.renderManager.time.time * 100.0).toDouble()
        timePointer.startX = x
        timePointer.endX = x
        if(timePointerCentering){
            content.layoutX = -(timePointer.startX - width/5)
        }
    }

    private fun initCustomPanning(){
        var mouseXOnStartDrag = 0.0
        var mouseYOnStartDrag = 0.0
        var layoutXOnStartDrag = 0.0
        var layoutYOnStartDrag = 0.0

        setOnMousePressed { event ->
            if(event.button == MouseButton.MIDDLE){
                mouseXOnStartDrag = event.screenX
                mouseYOnStartDrag = event.screenY
                layoutXOnStartDrag = content.layoutX
                layoutYOnStartDrag = content.layoutY
            }
        }

        addEventFilter(MouseEvent.MOUSE_DRAGGED, { event ->
            if(event.isMiddleButtonDown){
                content.layoutX = minOf(layoutXOnStartDrag + (event.screenX - mouseXOnStartDrag), 0.0)
                content.layoutY = minOf(layoutYOnStartDrag + (event.screenY - mouseYOnStartDrag), 0.0)
            }
        })
    }
}
