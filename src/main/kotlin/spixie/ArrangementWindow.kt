package spixie

import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import org.apache.commons.math3.fraction.Fraction
import java.awt.Color
import java.awt.image.BufferedImage

class ArrangementWindow: Pane(), WorkingWindowOpenableContent, SpixieHashable {
    val content = Group()
    val blocks = Group()
    val cursor = Line(-0.5, 0.0, -0.5, 10000.0)
    val zoom = FractionImmutablePointer(Fraction(64))
    val timePointer = Line(-0.5, 0.0, -0.5, 10000.0)
    var timePointerCentering = false
        set(value) {
            field = value
            if(value){
                updateTimePointer()
            }
        }
    val waveform = Canvas(1.0, 600.0)

    init {
        widthProperty().addListener { _, _, newValue ->
            waveform.width = newValue.toDouble() + 200.0
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

        addEventHandler(MouseEvent.MOUSE_PRESSED, { event ->
            if(event.button == MouseButton.PRIMARY){
                Main.world.time.time = (cursor.startX-0.5)*64/100.0/zoom.value.toDouble()
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

                    for (child in blocks.children) {
                        if(child is ArrangementBlock){
                            child.updateZoom()
                        }
                    }
                    redrawWaveform()
                    updateTimePointer()
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

                    for (child in blocks.children) {
                        if(child is ArrangementBlock){
                            child.updateZoom()
                        }
                    }
                    redrawWaveform()
                    updateTimePointer()
                }
            }
            event.consume()
        }

        val grid = initGrid()

        content.children.addAll(waveform, grid, blocks, cursor, timePointer)
        children.addAll(content)

        clip = Rectangle(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        content.layoutXProperty().addListener { _, _, _ ->
            redrawWaveform()
        }
        content.layoutYProperty().addListener { _, _, y ->
            waveform.layoutY = -y.toDouble()
        }

        blocks.children.add(ArrangementBlock(zoom))
    }

    private fun updateCursor(event:MouseEvent){
        val sceneToLocal = content.sceneToLocal(event.sceneX, event.sceneY)
        val x = Math.round(sceneToLocal.x/25)*25 + 0.5
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

    private @Volatile var nowRedrawing = false
    private @Volatile var restartRedrawingWaveform = false
    fun redrawWaveform(){
        if(!nowRedrawing){
            nowRedrawing = true
            Thread(Runnable {
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
                val toFXImage = SwingFXUtils.toFXImage(bufferedImage, null)
                runInUIAndWait {
                    waveform.graphicsContext2D.drawImage(toFXImage, 0.0, 0.0)
                    waveform.layoutX = newWaveformLayoutX
                }
                nowRedrawing = false
                if(restartRedrawingWaveform){
                    restartRedrawingWaveform =false
                    redrawWaveform()
                }
            }).start()
        }else{
            restartRedrawingWaveform = true
        }
    }

    private fun initGrid():Group{
        val group = Group()
        for(i in 0..99){
            val line = Line(0.0, i * 100.0 + 0.5, 10000.0, i * 100.0 + 0.5)
            line.opacity = 0.4
            group.children.addAll(line)
        }

        for(i in 0..34){
            val lineMajor = Line(i * 400.0 + 0.5, 0.0, i * 400.0 + 0.5, 10000.0)
            lineMajor.opacity = 0.4
            val line1 = Line(i * 400.0 + 0.5+100, 0.0, i * 400.0 + 0.5+100, 10000.0)
            line1.opacity = 0.1
            val line2 = Line(i * 400.0 + 0.5+200, 0.0, i * 400.0 + 0.5+200, 10000.0)
            line2.opacity = 0.1
            val line3 = Line(i * 400.0 + 0.5+300, 0.0, i * 400.0 + 0.5+300, 10000.0)
            line3.opacity = 0.1
            group.children.addAll(lineMajor, line1, line2, line3)
        }
        return group
    }

    override fun spixieHash(): Long {
        var hash = magic.toLong()
        for (block in blocks.children) {
            if(block is ArrangementBlock){
                hash = hash mix block.spixieHash()
            }
        }
        return hash
    }
}
