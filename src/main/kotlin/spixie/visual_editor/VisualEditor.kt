package spixie.visual_editor

import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.CubicCurve
import javafx.stage.Screen
import spixie.Main
import spixie.WorkingWindowOpenableContent
import spixie.visual_editor.components.Result
import kotlin.math.absoluteValue

class VisualEditor: Pane(), WorkingWindowOpenableContent {
    val content = Group()
    val components = Group()
    val connects = Group()
    val inputToOutputConnection = hashMapOf<ComponentPin<*>, ComponentPin<*>>()
    var time = 0.0
    private set

    init {
        style = "-fx-background-color: #FFFFFFFF;"

        initCustomPanning()
        homeLayout()

        children.addAll(content)

        setOnMouseClicked { event ->
            if(event.button == MouseButton.SECONDARY){
                val screenToLocal = components.screenToLocal(event.screenX, event.screenY)
                ComponentsList(screenToLocal.x, screenToLocal.y, content.children) { result ->
                    result.magneticRelocate(screenToLocal.x - result.width / 2, screenToLocal.y)
                    components.children.add(result)
                }
            }
        }

        components.children.add(Result())

        setOnKeyReleased { event ->
            if(event.code == KeyCode.HOME){
                homeLayout()
                reconnectPins()
                event.consume()
            }
        }

        content.children.addAll(components, connects)
    }

    fun render(time:Double): ParticleArray {
        this.time = time
        components.children.forEach {
            if(it is Result){
                return it.getParticles()
            }
        }
        return ParticleArray()
    }

    fun reconnectPins(){
        connects.children.clear()
        layout()
        inputToOutputConnection.forEach { input, output ->
            connectPins(output, input)
        }
        Main.renderManager.requestRender()
    }

    fun homeLayout(){
        val visualBounds = Screen.getPrimary().visualBounds
        content.layoutX = visualBounds.width/2
        content.layoutY = visualBounds.height/2
        updateBackgroundGrid()
    }

    fun connectPins(pin1: ComponentPin<*>, pin2: ComponentPin<*>){
        val cubicCurve = CubicCurve()
        val aBounds = pin1.component.localToParent(pin1.component.content.localToParent(pin1.localToParent(pin1.circle.boundsInParent)))
        val bBounds = pin2.component.localToParent(pin2.component.content.localToParent(pin2.localToParent(pin2.circle.boundsInParent)))
        cubicCurve.startX = (aBounds.minX + aBounds.maxX) / 2
        cubicCurve.startY = (aBounds.minY + aBounds.maxY) / 2
        cubicCurve.endX = (bBounds.minX + bBounds.maxX) / 2
        cubicCurve.endY = (bBounds.minY + bBounds.maxY) / 2
        cubicCurve.controlX1 = cubicCurve.startX + (cubicCurve.endX - cubicCurve.startX).absoluteValue.coerceIn(64.0..800.0)/2
        cubicCurve.controlY1 = cubicCurve.startY
        cubicCurve.controlX2 = cubicCurve.endX - (cubicCurve.endX - cubicCurve.startX).absoluteValue.coerceIn(64.0..800.0)/2
        cubicCurve.controlY2 = cubicCurve.endY
        cubicCurve.fill = Color.TRANSPARENT
        cubicCurve.strokeWidth = 4.0
        cubicCurve.stroke = Color.DARKVIOLET
        connects.children.add(cubicCurve)

        cubicCurve.isMouseTransparent = true
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

        addEventHandler(MouseEvent.MOUSE_DRAGGED, { event ->
            if(event.isMiddleButtonDown){
                content.layoutX = layoutXOnStartDrag + (event.screenX - mouseXOnStartDrag)
                content.layoutY = layoutYOnStartDrag + (event.screenY - mouseYOnStartDrag)

                updateBackgroundGrid()
            }
        })
    }

    private fun updateBackgroundGrid(){
        style = "-fx-background-color: #FFFFFFFF, linear-gradient(from ${content.layoutX+0.5}px 0px to ${content.layoutX+16.5}px 0px, repeat, #00000022 5%, transparent 5%),linear-gradient(from 0px ${content.layoutY+0.5}px to 0px ${content.layoutY+16.5}px, repeat, #00000022 5%, transparent 5%);"
    }
}