package spixie

import javafx.scene.Group
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.shape.Line
import org.apache.commons.math3.fraction.Fraction

class ArrangementWindow: Pane(), WorkingWindowOpenableContent, SpixieHashable {
    val content = Group()
    val blocks = Group()
    val cursor = Line(-0.5, 0.0, -0.5, 10000.0)
    val zoom = FractionImmutablePointer(Fraction(64))
    val timePointer = Line(-0.5, 0.0, -0.5, 10000.0)

    init {
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
                    zoom.value = zoom.value.divide(2)
                    for (child in blocks.children) {
                        if(child is ArrangementBlock){
                            child.updateZoom()
                        }
                    }
                    updateTimePointer()
                }
            }
            if(event.deltaY>0){
                if(zoom.value.compareTo(Fraction(4096)) != 0){
                    zoom.value = zoom.value.multiply(2)
                    for (child in blocks.children) {
                        if(child is ArrangementBlock){
                            child.updateZoom()
                        }
                    }
                    updateTimePointer()
                }
            }
            event.consume()
        }

        val grid = initGrid()

        content.children.addAll(grid, blocks, cursor, timePointer)
        children.addAll(content)





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
