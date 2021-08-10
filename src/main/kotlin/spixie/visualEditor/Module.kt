package spixie.visualEditor

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.CubicCurve
import javafx.scene.shape.Rectangle
import javafx.scene.shape.StrokeLineCap
import spixie.renderManager
import spixie.static.initCustomPanning
import spixie.visualEditor.components.ParticlesResult
import spixie.visualEditor.components.RenderComponent
import spixie.visualEditor.pins.ComponentPin
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class Module {
    val contentPane = Pane()

    private val components = Group()

    val content = Group()

    private val connects = Group()

    private val selectionRectangle = Rectangle().apply {
        fill = Color.TRANSPARENT
        stroke = Color.BLACK
    }

    var selectedComponents = arrayOf<EditorComponent>()
        set(value) {
            selectedComponents.forEach { it.selected = false }
            field=value
            selectedComponents.forEach { it.selected = true }
        }

    init {
        contentPane.initCustomPanning(content, true)
        content.children.addAll(components, connects, selectionRectangle)
        contentPane.apply { children.addAll(content) }


        var selectionRectangleStartPoint = Point2D(0.0, 0.0)
        contentPane.setOnMousePressed { event ->
            if(event.button == MouseButton.PRIMARY) {
                selectionRectangleStartPoint = components.screenToLocal(event.screenX, event.screenY)

                selectionRectangle.x = selectionRectangleStartPoint.x
                selectionRectangle.y = selectionRectangleStartPoint.y
                selectionRectangle.width = 0.0
                selectionRectangle.height = 0.0
                selectionRectangle.isVisible = true

                val componentsUnderCursor = components.children
                        .map { it as EditorComponent }
                        .filter { it.boundsInParent.intersects(selectionRectangle.boundsInLocal) }
                        .toTypedArray()

                if(componentsUnderCursor.isEmpty() || !componentsUnderCursor.all { selectedComponents.contains(it) }) {
                    selectedComponents = componentsUnderCursor
                }
            }
        }

        contentPane.setOnMouseDragged { event ->
            if(event.button == MouseButton.PRIMARY) {
                val selectionRectangleEndPoint = components.screenToLocal(event.screenX, event.screenY)
                selectionRectangle.x = min(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x)
                selectionRectangle.y = min(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y)
                selectionRectangle.width = max(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x) - min(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x)
                selectionRectangle.height = max(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y) - min(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y)
                selectedComponents = components.children
                        .map { it as EditorComponent }
                        .filter { it.boundsInParent.intersects(selectionRectangle.boundsInLocal) }
                        .toTypedArray()
            }
        }

        contentPane.setOnMouseClicked { event ->
            if(event.button == MouseButton.SECONDARY) {
                val point2D = components.screenToLocal(event.screenX, event.screenY)
                openComponentsList(point2D) { result->
                    result.magneticRelocate(point2D.x - result.width / 2, point2D.y)
                    addComponent(result)
                }
            }
        }

        contentPane.setOnMouseReleased { event ->
            if(event.button == MouseButton.PRIMARY) {
                selectionRectangle.isVisible = false
            }
        }

        contentPane.clip = Rectangle(Double.MAX_VALUE, Double.MAX_VALUE)

        content.layoutXProperty().addListener { _, _, _ ->
            updateBackgroundGrid()
        }
        content.layoutYProperty().addListener { _, _, _ ->
            updateBackgroundGrid()
        }
    }

    fun openComponentsList(point2D: Point2D, result: (editorComponent: EditorComponent) -> Unit) {
        ComponentsList(point2D.x, point2D.y, content.children) {
            result(it)
        }
    }

    private fun clearComponents(){
        components.children.clear()
    }

    fun addComponent(editorComponent: EditorComponent){
        components.children.add(editorComponent)
        editorComponent.conneectionsChanged
                .startWith(Unit)
                .debounce(17L, TimeUnit.MILLISECONDS)
                .observeOn(JavaFxScheduler.platform())
                .subscribe{
                    reconnectPins()
                }
        editorComponent.disconnectPinRequest.subscribe { pinForDisconnect->
            components.children.forEach { component ->
                if(component is EditorComponent){
                    component.inputPins.forEach { pin ->
                        if(pin == pinForDisconnect){
                            pin.connections.clear()
                        }
                        if(pin.connections.contains(pinForDisconnect)){
                            pin.connections.remove(pinForDisconnect)
                        }
                    }
                }
            }
            editorComponent.conneectionsChanged.onNext(Unit)
        }
        editorComponent.relocateSelectedRequests.subscribe { (x, y)->
            selectedComponents.forEach {
                it.relativelyMagneticRelocate(x, y)
            }
        }
    }

    fun removeComponent(editorComponent: EditorComponent){
        components.children.remove(editorComponent)
    }

    fun findRenderComponent(): RenderComponent {
        val result = components.children.find { it is RenderComponent } ?: throw Exception("Render component dont exist")
        return result as RenderComponent
    }

    fun findFinalComponentNode(): Node {
        return components.children.find { it is RenderComponent || it is ParticlesResult } ?: throw Exception("Final component dont exist")
    }

    fun reconnectPins(){
        connects.children.clear()
        contentPane.layout()
        components.children.forEach { component ->
            if(component is EditorComponent){
                component.inputPins.forEach { pin1 ->
                    pin1.connections.forEach { pin2->
                        connectPins(pin2, pin1, Color.DARKVIOLET)
                    }
                }
            }
        }
        renderManager.requestRender()
    }

    private fun connectPins(outputPin: ComponentPin, inputPin: ComponentPin, color: Color){
        val cubicCurve = CubicCurve()
        val aBounds = outputPin.editorComponent.localToParent(outputPin.editorComponent.content.localToParent(outputPin.localToParent(outputPin.circle.boundsInParent)))
        val bBounds = inputPin.editorComponent.localToParent(inputPin.editorComponent.content.localToParent(inputPin.localToParent(inputPin.circle.boundsInParent)))
        cubicCurve.startX = (aBounds.minX + aBounds.maxX) / 2
        cubicCurve.startY = (aBounds.minY + aBounds.maxY) / 2
        cubicCurve.endX = (bBounds.minX + bBounds.maxX) / 2
        cubicCurve.endY = (bBounds.minY + bBounds.maxY) / 2
        cubicCurve.controlX1 = cubicCurve.startX + (cubicCurve.endX - cubicCurve.startX).absoluteValue.coerceIn(42.0..128.0)/2
        cubicCurve.controlY1 = cubicCurve.startY
        cubicCurve.controlX2 = cubicCurve.endX - (cubicCurve.endX - cubicCurve.startX).absoluteValue.coerceIn(42.0..128.0)/2
        cubicCurve.controlY2 = cubicCurve.endY
        cubicCurve.fill = Color.TRANSPARENT
        cubicCurve.strokeWidth = 3.0
        cubicCurve.stroke = color
        connects.children.add(cubicCurve)

        cubicCurve.isMouseTransparent = true
        cubicCurve.strokeLineCap = StrokeLineCap.ROUND
    }

    private fun updateBackgroundGrid(){
        contentPane.style = "-fx-background-color: #FFFFFFFF, linear-gradient(from ${content.layoutX+0.5}px 0px to ${content.layoutX+(VE_GRID_CELL_SIZE/2.0+0.5)}px 0px, repeat, #00000022 5%, transparent 5%),linear-gradient(from 0px ${content.layoutY+0.5}px to 0px ${content.layoutY+(VE_GRID_CELL_SIZE/2.0+0.5)}px, repeat, #00000022 5%, transparent 5%);"
    }
}