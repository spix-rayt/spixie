package spixie

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Paint
import javafx.scene.shape.CubicCurve
import spixie.components.Circle
import spixie.components.Graph
import spixie.components.ParticleSpray

class VisualEditor: Pane(), WorkingWindowOpenableContent, SpixieHashable {
    val content = Group()
    val components = Group()
    val connects = Group()

    init {
        style = "-fx-background-color: #FFFFFFFF;"

        initCustomPanning()

        children.addAll(content)

        val contextMenu = ContextMenu()
        val menuItemParticleSpray = MenuItem("Particle Spray")
        var menuItemCircle = MenuItem("Circle")
        var menuItemGraph = MenuItem("GraphWindow")
        contextMenu.items.addAll(menuItemCircle, menuItemParticleSpray, menuItemGraph)

        menuItemParticleSpray.setOnAction {
            val screenToLocal = content.screenToLocal(contextMenu.anchorX, contextMenu.anchorY)
            components.children.addAll(ParticleSpray(screenToLocal.x, screenToLocal.y))
            Main.world.clearCache()
        }

        menuItemCircle.setOnAction {
            val screenToLocal = content.screenToLocal(contextMenu.anchorX, contextMenu.anchorY)
            val circle = Circle(screenToLocal.x, screenToLocal.y)
            circle.onValueInputOutputConnected = { a, b ->
                if(a is Node && b is Node){
                    connectInputOutput(a,b)
                }
            }
            components.children.addAll(circle)
            Main.world.clearCache()
        }

        menuItemGraph.setOnAction {
            val screenToLocal = content.screenToLocal(contextMenu.anchorX, contextMenu.anchorY)
            val graph = Graph(screenToLocal.x, screenToLocal.y)
            graph.onValueInputOutputConnected = { a,b ->
                if(a is Node && b is Node){
                    connectInputOutput(a,b)
                }
            }
            components.children.addAll(graph)
            Main.world.clearCache()
        }

        setOnContextMenuRequested { event ->
            contextMenu.show(this, event.screenX, event.screenY)
        }

        setOnMousePressed {
            contextMenu.hide()
        }

        setOnKeyReleased { event ->
            if(event.code == KeyCode.HOME){
                content.layoutX = 0.0
                content.layoutY = 0.0
                event.consume()
            }
        }

        content.children.addAll(components, connects)
    }

    fun connectInputOutput(input:Node, output:Node){
        val cubicCurve = CubicCurve()
        val aBounds = connects.screenToLocal(input.localToScreen(input.layoutBounds))
        val bBounds = connects.screenToLocal(output.localToScreen(output.layoutBounds))
        cubicCurve.startX = (aBounds.minX + aBounds.maxX) / 2
        cubicCurve.startY = (aBounds.minY + aBounds.maxY) / 2
        cubicCurve.endX = (bBounds.minX + bBounds.maxX) / 2
        cubicCurve.endY = (bBounds.minY + bBounds.maxY) / 2
        cubicCurve.controlX1 = cubicCurve.startX
        cubicCurve.controlY1 = cubicCurve.startY
        cubicCurve.controlX2 = cubicCurve.endX
        cubicCurve.controlY2 = cubicCurve.endY
        cubicCurve.fill = Paint.valueOf("TRANSPARENT")
        cubicCurve.strokeWidth = 1.0
        cubicCurve.stroke = Paint.valueOf("BLACK")
        connects.children.add(cubicCurve)
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

    override fun spixieHash(): Long {
        var hash = magic.toLong()
        for (component in components.children) {
            if(component is VisualEditorComponent){
                hash = hash mix component.spixieHash()
            }
        }
        return hash
    }
}