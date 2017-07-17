package spixie

import javafx.scene.Group
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.shape.Line

class ArrangementWindow: Pane(), WorkingWindowOpenableContent, SpixieHashable {
    val content = Group()
    val blocks = Group()
    val cursor = Line(-0.5, 0.0, -0.5, 10000.0)
    init {
        style = "-fx-background-color: #FFFFFFFF;"

        initCustomPanning()


        val timePointer = Line(-0.5, 0.0, -0.5, 10000.0)
        timePointer.strokeWidth = 4.0
        Main.world.time.onTimeChanged { time ->
            timePointer.startX = Math.round(time*400.0).toDouble()
            timePointer.endX = Math.round(time*400.0).toDouble()
        }

        setOnMouseMoved { event ->
            updateCursor(event)
        }

        addEventFilter(MouseEvent.MOUSE_DRAGGED, { event ->
            updateCursor(event)
            if(event.isControlDown){
                Main.world.time.time = (cursor.startX-0.5)/400.0
            }
        })

        addEventHandler(MouseEvent.MOUSE_PRESSED, { event ->
            if(event.button == MouseButton.PRIMARY){
                Main.world.time.time = (cursor.startX-0.5)/400.0
            }
        })

        val grid = initGrid()

        content.children.addAll(grid, blocks, cursor, timePointer)
        children.addAll(content)





        blocks.children.add(ArrangementBlock())
    }

    private fun updateCursor(event:MouseEvent){
        val sceneToLocal = content.sceneToLocal(event.sceneX, event.sceneY)
        val x = Math.round(sceneToLocal.x/25)*25 + 0.5
        cursor.startX = x
        cursor.endX = x
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

    override fun appendSpixieHash(hash: StringBuilder): StringBuilder {
        for (block in blocks.children) {
            if(block is ArrangementBlock){
                block.appendSpixieHash(hash)
            }
        }
        return hash
    }
}
