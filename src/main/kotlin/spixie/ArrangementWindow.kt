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
import spixie.components.Circle
import spixie.components.ParticleSpray
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.FloatBuffer

class ArrangementWindow: Pane(), WorkingWindowOpenableContent, SpixieHashable {
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

    val redrawWaveform = SerialWorker {
        val newWaveformLayoutX = -content.layoutX - 100
        val startTime = (-content.layoutX - 100)*64/100.0/zoom.value.toDouble()
        val endTime = (-content.layoutX + width + 100)*64/100.0/zoom.value.toDouble()

        val startSecond = startTime*3600/Main.world.bpm.value.value/60
        val endSecond = endTime*3600/Main.world.bpm.value.value/60

        val secondsInPixel = (endSecond - startSecond) / waveform.width


        val bufferedImage = BufferedImage(waveform.width.toInt(), waveform.height.toInt(), BufferedImage.TYPE_4BYTE_ABGR)
        val g = bufferedImage.graphics
        g.color = Color.WHITE
        g.fillRect(0, 0, bufferedImage.width, bufferedImage.height)
        g.color = Color(66, 170, 255)
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
        runInUIAndWait {
            waveform.graphicsContext2D.drawImage(toFXImage, 0.0, 0.0)
            waveform.layoutX = newWaveformLayoutX
        }
    }

    val updateGrid = SerialWorker {
        val lines = ArrayList<Line>()
        val x = -content.layoutX.toInt() / 400
        val y = -content.layoutY.toInt() / 100
        for(i in y-10..y+20){
            val line = Line((x-3) * 400.0, i * 100.0 + 0.5, (x-3) * 400.0 + 10000.0, i * 100.0 + 0.5)
            line.opacity = 0.4
            lines.add(line)
        }
        val startY = (y-10) * 100.0
        for(i in x-3..x+8){
            val lineMajor = Line(i * 400.0 + 0.5, startY, i * 400.0 + 0.5, startY + 10000.0)
            lineMajor.opacity = 0.4
            lines.add(lineMajor)
            val line1 = Line(i * 400.0 + 0.5+100, startY, i * 400.0 + 0.5+100, startY + 10000.0)
            line1.opacity = 0.1
            lines.add(line1)
            val line2 = Line(i * 400.0 + 0.5+200, startY, i * 400.0 + 0.5+200, startY + 10000.0)
            line2.opacity = 0.1
            lines.add(line2)
            val line3 = Line(i * 400.0 + 0.5+300, startY, i * 400.0 + 0.5+300, startY + 10000.0)
            line3.opacity = 0.1
            lines.add(line3)
        }
        runInUIAndWait {
            grid.children.setAll(lines)
            cursor.startY = startY
            cursor.endY = startY + 10000.0
            timePointer.startY = startY
            timePointer.endY = startY + 10000.0
        }
    }

    val updateGraphs = SerialWorker {
        updateGraph(null) // update all graphs
    }

    fun updateGraph(only: ArrangementGraph?){
        val newGraphsLayoutX = -content.layoutX - 100
        val startTime = (-content.layoutX - 100)*64/100.0/zoom.value.toDouble()
        val endTime = (-content.layoutX + width + 100)*64/100.0/zoom.value.toDouble()
        runInUIAndWait {
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
                    val y1 = graph.data.getValue(startTime + x*beatsInPixel)
                    val y2 = graph.data.getValue(startTime + (x+1.0)*beatsInPixel)
                    g.strokeLine(x.toDouble(), y1 * canvas.height, (x + 1).toDouble(), y2 * canvas.height)
                    x++
                }
            }
            if(only != null){
                redraw(only)
            }else{
                for (graph in graphs) {
                    redraw(graph.value)
                }
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
        style = "-fx-background-color: #FFFFFFFF;"

        initCustomPanning()



        timePointer.strokeWidth = 4.0
        Main.world.time.onTimeChanged {
            updateTimePointer()
        }

        setOnMouseMoved { event ->
            updateCursor(event)
        }

        addEventFilter(MouseEvent.MOUSE_DRAGGED, { event ->
            updateCursor(event)
            if(event.isControlDown){
                val screenToLocal = content.screenToLocal(event.screenX, event.screenY)
                Main.world.time.time = screenToLocal.x*64/100.0/zoom.value.toDouble()
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
                            val restartAudio = Main.audio.isPlayed()
                            if(restartAudio){
                                Main.audio.pause()
                            }
                            Main.world.time.time = (cursor.startX-0.5)*64/100.0/zoom.value.toDouble()
                            if(restartAudio){
                                Main.audio.play(Duration.seconds(Main.world.time.frame/60.0))
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

                    redrawWaveform.run()
                    updateGraphs.run()
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

                    redrawWaveform.run()
                    updateGraphs.run()
                    updateTimePointer()
                    selectionBlock.updateZoom()
                }
            }
            event.consume()
        }

        updateGrid.run()

        content.children.addAll(waveform, grid, graphCanvases, selectionBlock, cursor, timePointer, graphBuilderGroup)
        children.addAll(content)

        clip = Rectangle(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        content.layoutXProperty().addListener { _, _, _ ->
            redrawWaveform.run()
            updateGrid.run()
            updateGraphs.run()
        }
        content.layoutYProperty().addListener { _, _, y ->
            waveform.layoutY = -y.toDouble()
            updateGrid.run()
            updateGraphs.run()
        }
    }

    private fun updateCursor(event:MouseEvent){
        val sceneToLocal = content.sceneToLocal(event.sceneX, event.sceneY)
        val x = Math.round(sceneToLocal.x/6.25)*6.25 + 0.5
        cursor.startX = x
        cursor.endX = x
    }

    private fun updateTimePointer(){
        val x = Math.round(zoom.value.toDouble() / 64.0 * Main.world.time.time * 100.0).toDouble()
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

    fun render(): FloatBuffer {
        for (component in visualEditor.components.children) {
            if (component is ParticleSpray) {
                component.autoStepParticles(Math.round(Main.world.time.time*100).toInt())
            }
        }
        var particles = 0

        for (component in visualEditor.components.children) {
            if(component is ParticleSpray){
                particles+=component.particles.size
            }
            if(component is Circle){
                particles+=Math.ceil(component.count.value.value).toInt()
            }
        }
        val renderBufferBuilder = RenderBufferBuilder(particles)


        for (component in visualEditor.components.children) {
            if(component is ParticleSpray){
                for (particle in component.particles) {
                    renderBufferBuilder.addParticle(particle.x, particle.y, particle.size, particle.red, particle.green, particle.blue, particle.alpha)
                }
            }
            if(component is Circle){
                component.render(renderBufferBuilder)
            }
        }
        return renderBufferBuilder.complete()
    }

    override fun spixieHash(): Long {
        return visualEditor.spixieHash()
    }
}
