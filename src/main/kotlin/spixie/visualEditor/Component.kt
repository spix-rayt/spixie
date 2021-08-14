package spixie.visualEditor

import com.google.gson.JsonElement
import io.reactivex.subjects.PublishSubject
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import spixie.gson
import spixie.visualEditor.pins.ComponentPin
import spixie.visualEditor.pins.ComponentPinNumber
import kotlin.math.roundToInt

abstract class Component: Region() {
    val parameters = arrayListOf<Node>()

    val inputPins = arrayListOf<ComponentPin>()

    val outputPins = arrayListOf<ComponentPin>()

    val content = Group()

    private var dragDelta = Point2D(0.0, 0.0)

    val connectionsChanged = PublishSubject.create<Unit>()

    val disconnectPinRequest = PublishSubject.create<ComponentPin>()

    var serializeId = 0

    var selected = false
        set(value) {
            field=value
            border = if(value) {
                Border(BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(2.0)))
            } else {
                Border(BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(1.0)))
            }
        }

    init {
        selected = false
        style = "-fx-background-color: #FFFFFFFF;"

        setOnMouseClicked { event ->
            event.consume()
        }

        setOnMousePressed { event ->
            if(event.button == MouseButton.PRIMARY) {
                dragDelta = Point2D(layoutX - event.sceneX, layoutY - event.sceneY)
            }
        }

        children.addAll(content)
    }

    fun magneticRelocate(x: Double, y:Double) {
        val newX = (x / VE_GRID_CELL_SIZE).roundToInt() * VE_GRID_CELL_SIZE
        val newY = (y / VE_GRID_CELL_SIZE).roundToInt() * VE_GRID_CELL_SIZE
        if(layoutX != newX || layoutY != newY) {
            relocate(newX, newY)
            connectionsChanged.onNext(Unit)
        }
    }

    fun relativelyMagneticRelocate(x: Double, y:Double) {
        magneticRelocate(layoutX + x, layoutY + y)
    }

    fun getReadableName(): String {
        return javaClass.simpleName.replace(Regex("[A-Z]"), { matchResult -> " ${matchResult.value}" })
    }

    fun updateUI() {
        content.children.clear()
        content.children.addAll(
                Label(getReadableName()).apply {
                    style="-fx-font-weight: bold; -fx-font-style: italic;"
                    alignment = Pos.CENTER
                    prefWidth = VE_PIN_WIDTH + VE_KEK + 1.0
                    prefHeight = VE_GRID_CELL_SIZE
                }
        )
        content.children.addAll(parameters)
        content.children.addAll(inputPins)
        content.children.addAll(outputPins)

        parameters.forEachIndexed { index, node ->
            node.apply {
                this.layoutX = 0.0
                this.layoutY = index*VE_GRID_CELL_SIZE+VE_GRID_CELL_SIZE
                this.minWidth(VE_PIN_WIDTH - VE_GRID_CELL_SIZE)
                this.maxWidth(VE_PIN_WIDTH - VE_GRID_CELL_SIZE)

                this.minHeight(VE_GRID_CELL_SIZE)
                this.maxHeight(VE_GRID_CELL_SIZE)
            }
        }

        val visibleInputPins = inputPins.filter { it.isVisible }

        visibleInputPins.forEachIndexed { index, pin ->
            pin.layoutX = 0.0
            pin.layoutY = index*VE_GRID_CELL_SIZE+VE_GRID_CELL_SIZE + parameters.size*VE_GRID_CELL_SIZE
            pin.updateUI()
        }

        outputPins.forEachIndexed { index, pin ->
            pin.layoutX = VE_KEK
            pin.layoutY = index*VE_GRID_CELL_SIZE+VE_GRID_CELL_SIZE + visibleInputPins.size*VE_GRID_CELL_SIZE + parameters.size*VE_GRID_CELL_SIZE
            pin.updateUI()
        }


        prefWidth = VE_PIN_WIDTH + VE_KEK + 1.0
        prefHeight = getHeightInCells() * VE_GRID_CELL_SIZE + 1.0
        width = VE_PIN_WIDTH + VE_KEK + 1.0
        height = getHeightInCells() * VE_GRID_CELL_SIZE + 1.0
        connectionsChanged.onNext(Unit)
    }

    fun findInputPinByName(pinName: String): ComponentPin? {
        return inputPins.find { it.name == pinName }
    }

    fun findOutputPinByName(pinName: String): ComponentPin? {
        return outputPins.find { it.name == pinName }
    }

    protected open fun getHeightInCells(): Int {
        val visibleInputPins = inputPins.filter { it.isVisible }
        return visibleInputPins.size + parameters.size + outputPins.size + 1
    }

    fun calcRecursiveMaxHeight(): Double {
        return (inputPins.flatMap { it.connections }.map { it.component.calcRecursiveMaxHeight() } + (height - 1.0)).maxOrNull() ?: 0.0
    }

    fun createSame(): Component {
        return this::class.java.getDeclaredConstructor().newInstance()
    }

    fun getData(): JsonElement {
        return gson.toJsonTree(
            inputPins.mapNotNull { it as? ComponentPinNumber }
                .map { it.name to it.valueControl?.value }
                .filter { it.second != null }
                .toMap()
        )
    }

    fun setData(jsonElement: JsonElement) {
        val jsonObject = jsonElement.asJsonObject
        inputPins.mapNotNull { it as? ComponentPinNumber }
            .forEach {
                val value = jsonObject[it.name]
                if(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
                    it.valueControl?.value = value.asJsonPrimitive.asDouble
                }
            }
    }

    fun toSerialized(): SerializedComponent {
        return SerializedComponent(serializeId, this::class.simpleName, getData())
    }

    data class SerializedComponent(val serializeId: Int, val clazz: String?, val data: JsonElement)

    fun getConnectionsForSerialization(): List<SerializedConnection> {
        return inputPins.flatMap { inputPin ->
            inputPin.connections.map { sourceOutputPin ->
                SerializedConnection(inputPin.component.serializeId, inputPin.name, sourceOutputPin.component.serializeId, sourceOutputPin.name)
            }
        }
    }

    data class SerializedConnection(val inputSerializeId: Int, val inputPinName: String, val sourceSerializeId: Int, val sourcePinName: String)

    companion object {
        fun fromSerialized(serializedComponent: SerializedComponent): Component {
            val clazz = Class.forName("spixie.visualEditor.components.${serializedComponent.clazz}")
            val component = clazz.getDeclaredConstructor().newInstance() as Component
            component.serializeId = serializedComponent.serializeId
            component.setData(serializedComponent.data)
            return component
        }
    }
}