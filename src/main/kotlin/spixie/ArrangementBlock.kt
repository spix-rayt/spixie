package spixie

import javafx.scene.Cursor
import javafx.scene.input.MouseButton
import javafx.scene.layout.Region

class ArrangementBlock(): Region(), SpixieHashable {
    val visualEditor = VisualEditor()

    var strictWidth:Double
        get() = width
        set(value) {
            minWidth = value
            maxWidth = value
        }

    var strictHeight:Double
        get() = height
        set(value) {
            minHeight = value
            maxHeight = value
        }

    enum class Drag { BEGIN, END, NONE }

    init {
        strictWidth = 400.0
        strictHeight = 100.0

        setOnMouseMoved { event ->
            val mousePos = sceneToLocal(event.sceneX, event.sceneY)
            if(mousePos.x>strictWidth-10 || mousePos.x < 10){
                scene.cursor = Cursor.E_RESIZE
            }else{
                scene.cursor = Cursor.DEFAULT
            }
        }

        var mouseXOnStartDrag = 0.0
        var layoutXOnStartDrag = 0.0
        var strictWidthOnStartDrag = 0.0
        var dragByThe = Drag.NONE

        setOnMousePressed { event ->
            if(event.button == MouseButton.PRIMARY){
                val mouseCoords = sceneToLocal(event.sceneX, event.sceneY)
                if(mouseCoords.x>strictWidth-10){
                    mouseXOnStartDrag = event.screenX
                    layoutXOnStartDrag = layoutX
                    strictWidthOnStartDrag = strictWidth
                    dragByThe = Drag.END
                    event.consume()
                }
                if(mouseCoords.x<10){
                    mouseXOnStartDrag = event.screenX
                    layoutXOnStartDrag = layoutX
                    strictWidthOnStartDrag = strictWidth
                    dragByThe = Drag.BEGIN
                    event.consume()
                }
            }
        }

        setOnMouseDragged { event ->
            if(event.isPrimaryButtonDown){
                var deltaX = Math.round((event.screenX - mouseXOnStartDrag)/25.0)*25.0
                if(dragByThe == Drag.BEGIN){
                    if(strictWidthOnStartDrag - deltaX < 25){
                        deltaX = strictWidthOnStartDrag - 25
                    }
                    if(layoutXOnStartDrag + deltaX < 0){
                        deltaX = -layoutXOnStartDrag
                    }
                    layoutX = layoutXOnStartDrag + deltaX
                    strictWidth = strictWidthOnStartDrag - deltaX
                }
                if(dragByThe == Drag.END){
                    if(strictWidthOnStartDrag + deltaX < 25){
                        deltaX = -strictWidthOnStartDrag + 25
                    }
                    strictWidth = strictWidthOnStartDrag + deltaX
                }
            }
        }

        setOnMouseReleased { event ->
            if(event.button == MouseButton.PRIMARY){
                dragByThe = Drag.NONE
            }
            if(event.button == MouseButton.SECONDARY){
                Main.workingWindow.nextOpen(visualEditor)
            }
        }

        setOnMouseExited { scene.cursor = Cursor.DEFAULT }
    }

    override fun spixieHash(): Long {
        return visualEditor.spixieHash() mix strictWidth.raw() mix layoutX.raw()
    }
}