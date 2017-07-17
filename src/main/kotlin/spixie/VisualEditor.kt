package spixie

import javafx.scene.Group
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import spixie.components.Circle
import spixie.components.ParticleSpray

class VisualEditor: Pane(), WorkingWindowOpenableContent, SpixieHashable {
    val content = Group()
    val components = Group()

    init {
        style = "-fx-background-color: #FFFFFFFF;"

        initCustomPanning()

        children.addAll(content)

        val contextMenu = ContextMenu()
        val menuItemParticleSpray = MenuItem("Particle Spray")
        var menuItemCircle = MenuItem("Circle")
        contextMenu.items.addAll(menuItemCircle, menuItemParticleSpray)

        menuItemParticleSpray.setOnAction {
            val screenToLocal = content.screenToLocal(contextMenu.anchorX, contextMenu.anchorY)
            components.children.addAll(ParticleSpray(screenToLocal.x, screenToLocal.y))
            Main.world.clearCache()
        }

        menuItemCircle.setOnAction {
            val screenToLocal = content.screenToLocal(contextMenu.anchorX, contextMenu.anchorY)
            components.children.addAll(Circle(screenToLocal.x, screenToLocal.y))
            Main.world.clearCache()
        }

        setOnContextMenuRequested { event ->
            contextMenu.show(this, event.screenX, event.screenY)
        }

        setOnMousePressed {
            contextMenu.hide()
        }

        content.children.addAll(components)
    }

    private fun initCustomPanning(){
        var mouseXOnStartDrag = 0.0
        var mouseYOnStartDrag = 0.0
        var layoutXOnStartDrag = 0.0
        var layoutYOnStartDrag = 0.0

        addEventHandler(MouseEvent.MOUSE_PRESSED, { event ->
            if(event.button == MouseButton.MIDDLE){
                mouseXOnStartDrag = event.screenX
                mouseYOnStartDrag = event.screenY
                layoutXOnStartDrag = content.layoutX
                layoutYOnStartDrag = content.layoutY
            }
        })

        addEventFilter(MouseEvent.MOUSE_DRAGGED, { event ->
            if(event.isMiddleButtonDown){
                content.layoutX = layoutXOnStartDrag + (event.screenX - mouseXOnStartDrag)
                content.layoutY = layoutYOnStartDrag + (event.screenY - mouseYOnStartDrag)
            }
        })
    }

    override fun appendSpixieHash(hash: StringBuilder):StringBuilder {
        hash.append("(")
        for (component in components.children) {
            if(component is VisualEditorComponent){
                component.appendSpixieHash(hash)
            }
        }
        hash.append(")")
        return hash
    }
}