package spixie.visualEditor

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.input.TransferMode
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import spixie.DragAndDropType
import spixie.dragAndDropObject
import spixie.renderManager
import spixie.static.initCustomPanning
import spixie.static.pickFirstConnectableByType
import spixie.visualEditor.components.RenderComponent
import spixie.visualEditor.pins.ComponentPin
import spixie.visualEditor.pins.ComponentPinParticleArray
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class Module {
    val contentPane = Pane()

    private val componentsContainer = Group()

    val content = Group()

    private val pinConnectionsLayer = Group()

    private val selectionRectangle = Rectangle().apply {
        fill = Color.TRANSPARENT
        stroke = Color.BLACK
    }

    var selectedComponents = arrayOf<Component>()
        set(value) {
            selectedComponents.forEach { it.selected = false }
            field=value
            selectedComponents.forEach { it.selected = true }
        }

    init {
        contentPane.initCustomPanning(content, true)
        content.children.addAll(componentsContainer, pinConnectionsLayer, selectionRectangle)
        contentPane.apply { children.addAll(content) }


        var selectionRectangleStartPoint = Point2D(0.0, 0.0)
        contentPane.setOnMousePressed { event ->
            if(event.button == MouseButton.PRIMARY) {
                selectionRectangleStartPoint = componentsContainer.screenToLocal(event.screenX, event.screenY)

                selectionRectangle.x = selectionRectangleStartPoint.x
                selectionRectangle.y = selectionRectangleStartPoint.y
                selectionRectangle.width = 0.0
                selectionRectangle.height = 0.0
                selectionRectangle.isVisible = true

                val componentsUnderCursor = componentsContainer.children
                        .map { it as Component }
                        .filter { it.boundsInParent.intersects(selectionRectangle.boundsInLocal) }
                        .toTypedArray()

                if(componentsUnderCursor.isEmpty() || !componentsUnderCursor.all { selectedComponents.contains(it) }) {
                    selectedComponents = componentsUnderCursor
                }
            }
        }

        contentPane.setOnMouseDragged { event ->
            if(event.button == MouseButton.PRIMARY) {
                val selectionRectangleEndPoint = componentsContainer.screenToLocal(event.screenX, event.screenY)
                selectionRectangle.x = min(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x)
                selectionRectangle.y = min(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y)
                selectionRectangle.width = max(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x) - min(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x)
                selectionRectangle.height = max(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y) - min(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y)
                selectedComponents = componentsContainer.children
                        .map { it as Component }
                        .filter { it.boundsInParent.intersects(selectionRectangle.boundsInLocal) }
                        .toTypedArray()
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

        contentPane.setOnDragOver { event ->
            if(event.gestureSource != this && event.dragboard.hasContent(DragAndDropType.PIN)) {
                event.acceptTransferModes(TransferMode.LINK)
            }
            event.consume()
        }


        contentPane.setOnDragDropped { event ->
            val dragboard = event.dragboard
            var success = false

            if(dragboard.hasContent(DragAndDropType.PIN)) {
                success = true
                (dragAndDropObject as? ComponentPin)?.let { dragged ->
                    openComponentBuilder(event.screenX, event.screenY, null, dragged::class.java) {
                        addComponent(it)
                        dragged.connectWith(it.outputPins.pickFirstConnectableByType(dragged::class.java)!!)
                        relocateAllComponents()
                    }
                }
            }
            event.isDropCompleted = success
            event.consume()
        }
    }

    private fun openComponentBuilder(screenX: Double, screenY: Double, inputFilter: Class<out ComponentPin>?, outputFilter: Class<out ComponentPin>?, result: (component: Component) -> Unit) {
        val point2D = content.screenToLocal(screenX, screenY)
        ComponentSelector(point2D.x, point2D.y, content.children, inputFilter, outputFilter) {
            result(it)
        }
    }

    private fun clearComponents() {
        componentsContainer.children.clear()
    }

    fun addComponent(component: Component) {
        componentsContainer.children.add(component)
        component.connectionsChanged
                .startWith(Unit)
                .debounce(17L, TimeUnit.MILLISECONDS)
                .observeOn(JavaFxScheduler.platform())
                .subscribe{
                    redrawPinConnections()
                }
        component.disconnectPinRequest.subscribe { pinForDisconnect->
            componentsContainer.children.forEach { component ->
                if(component is Component) {
                    component.inputPins.forEach { pin ->
                        if(pin == pinForDisconnect) {
                            pin.connections.clear()
                        }
                        if(pin.connections.contains(pinForDisconnect)) {
                            pin.connections.remove(pinForDisconnect)
                        }
                    }
                }
            }
            component.connectionsChanged.onNext(Unit)
        }
    }

    fun removeComponent(component: Component) {
        if(component !is RenderComponent) {
            componentsContainer.children.remove(component)
            component.inputPins.forEach { component.disconnectPinRequest.onNext(it) }
            component.outputPins.forEach { component.disconnectPinRequest.onNext(it) }
            relocateAllComponents()
        }
    }

    fun findRenderComponent(): RenderComponent {
        val result = componentsContainer.children.find { it is RenderComponent } ?: throw Exception("Render component dont exist")
        return result as RenderComponent
    }

    fun findRenderComponentNode(): Node {
        return componentsContainer.children.find { it is RenderComponent } ?: throw Exception("Render component dont exist")
    }

    private fun redrawPinConnections() {
        pinConnectionsLayer.children.clear()
        contentPane.layout()
        componentsContainer.children.forEach { component ->
            if(component is Component) {
                component.inputPins.forEach { pin1 ->
                    pin1.connections.forEach { pin2->
                        drawPinConnectionWire(pin2, pin1, Color.DARKVIOLET)
                    }
                }
            }
        }
        renderManager.requestRender()
    }

    private fun drawPinConnectionWire(outputPin: ComponentPin, inputPin: ComponentPin, color: Color) {
        val cubicCurve = CubicCurve()
        val aBounds = outputPin.component.localToParent(outputPin.component.content.localToParent(outputPin.localToParent(outputPin.circle.boundsInParent)))
        val bBounds = inputPin.component.localToParent(inputPin.component.content.localToParent(inputPin.localToParent(inputPin.circle.boundsInParent)))
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
        pinConnectionsLayer.children.add(cubicCurve)

        cubicCurve.isMouseTransparent = true
        cubicCurve.strokeLineCap = StrokeLineCap.ROUND

        val circle = Circle((cubicCurve.startX + cubicCurve.endX) / 2.0, (cubicCurve.startY + cubicCurve.endY) / 2.0, 5.0, Color.DARKVIOLET)
        circle.setOnMouseClicked { event ->
            if(event.button == MouseButton.SECONDARY) {
                if(outputPin is ComponentPinParticleArray && inputPin is ComponentPinParticleArray) {
                    openComponentBuilder(event.screenX, event.screenY, outputPin::class.java, inputPin::class.java) { newComponent ->
                        addComponent(newComponent)
                        inputPin.disconnectWith(outputPin)
                        inputPin.connectWith(newComponent.outputPins.pickFirstConnectableByType(inputPin::class.java)!!)
                        newComponent.inputPins.pickFirstConnectableByType(outputPin::class.java)?.connectWith(outputPin)
                        relocateAllComponents()
                    }
                }
            }
        }
        pinConnectionsLayer.children.add(circle)
    }

    private fun updateBackgroundGrid() {
        contentPane.style = "-fx-background-color: #FFFFFFFF, linear-gradient(from ${content.layoutX+0.5}px 0px to ${content.layoutX+(VE_GRID_CELL_SIZE/2.0+0.5)}px 0px, repeat, #00000022 5%, transparent 5%),linear-gradient(from 0px ${content.layoutY+0.5}px to 0px ${content.layoutY+(VE_GRID_CELL_SIZE/2.0+0.5)}px, repeat, #00000022 5%, transparent 5%);"
    }

    fun relocateAllComponents() {
        val components = componentsContainer.children.map { it as Component }
        val componentsForRemove = components.toMutableList()
        val finalComponent = components.first { it is RenderComponent }

        var x = 0.0
        var y = 0.0
        val column = arrayListOf(finalComponent)
        while (column.isNotEmpty()) {
            componentsForRemove.removeAll(column)
            val newColumn = arrayListOf<Component>()
            column.forEach { component ->
                component.relocate(x, y)
                y += component.height + VE_GRID_CELL_SIZE - 1.0
                newColumn.addAll(component.inputPins.flatMap { it.connections }.map { it.component }.distinct())
            }

            column.clear()
            column.addAll(newColumn)
            newColumn.clear()

            x -= (column.maxOfOrNull { it.width } ?: 0.0) + VE_GRID_CELL_SIZE - 1.0
            y = -((column.sumOf { it.calcRecursiveMaxHeight() }) / 2.0 + 0.5)
        }
        componentsForRemove.forEach {
            removeComponent(it)
        }
        redrawPinConnections()
    }

    fun toSerialized(): SerializedModule {
        val components = componentsContainer.children.map { it as Component }
        components.forEachIndexed { index, component -> component.serializeId = index }
        return SerializedModule(
            components.map { it.toSerialized() },
            components.flatMap { it.getConnectionsForSerialization() }
        )
    }

    data class SerializedModule(val components: List<Component.SerializedComponent>, val connections: List<Component.SerializedConnection>)

    companion object {
        fun fromSerialized(serializedModule: SerializedModule): Module {
            val newModule = Module()
            val componentById = hashMapOf<Int, Component>()
            serializedModule.components.forEach { serializedComponent ->
                val newComponent = Component.fromSerialized(serializedComponent)
                componentById[newComponent.serializeId] = newComponent
                newModule.addComponent(newComponent)
            }
            serializedModule.connections.forEach { serializedConnection ->
                val inputPin = componentById[serializedConnection.inputSerializeId]?.findInputPinByName(serializedConnection.inputPinName)
                val sourceOutputPin = componentById[serializedConnection.sourceSerializeId]?.findOutputPinByName(serializedConnection.sourcePinName)
                if(inputPin != null && sourceOutputPin != null) {
                    inputPin.connectWith(sourceOutputPin)
                }
            }
            newModule.relocateAllComponents()
            return newModule
        }
    }
}