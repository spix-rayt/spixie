package spixie.visualEditor.pins

import com.google.gson.JsonObject
import javafx.geometry.Pos
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import spixie.Core
import spixie.NumberControl
import spixie.static.DragAndDropType
import spixie.visualEditor.Component
import spixie.visualEditor.VE_GRID_CELL_SIZE
import spixie.visualEditor.VE_PIN_WIDTH
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

abstract class ComponentPin(val name: String): HBox() {
    private val backgroundCircle = Circle(VE_GRID_CELL_SIZE /2, VE_GRID_CELL_SIZE /2, 2.0, Color.BLACK)

    private val selectionCircle = Circle(VE_GRID_CELL_SIZE /2, VE_GRID_CELL_SIZE /2, 1.0, Color.WHITE)

    val component: Component
        get() {
            return this.parent.parent as Component
        }

    val circle = StackPane(backgroundCircle, selectionCircle).apply {
        minWidth = VE_GRID_CELL_SIZE
        maxWidth = VE_GRID_CELL_SIZE

        minHeight = VE_GRID_CELL_SIZE
        maxHeight = VE_GRID_CELL_SIZE
    }

    open val connections = mutableSetOf<ComponentPin>()

    protected val label = Label(name)

    val contextMenu = ContextMenu().apply {
        val unconnectAllItem = MenuItem("Unconnect all").apply {
            setOnAction {
                unconnectAll()
            }
        }
        items.add(unconnectAllItem)
    }

    fun unconnectAll() {
        component.disconnectPinRequest.onNext(this)
    }

    init {
        label.apply {
            minWidth = VE_PIN_WIDTH - VE_GRID_CELL_SIZE
            maxWidth = VE_PIN_WIDTH - VE_GRID_CELL_SIZE

            minHeight = VE_GRID_CELL_SIZE
            maxHeight = VE_GRID_CELL_SIZE
        }

        setOnContextMenuRequested { event->
            contextMenu.show(this, event.screenX, event.screenY)
            event.consume()
        }

        circle.apply {
            setOnMousePressed { event ->
                if(event.button == MouseButton.PRIMARY) event.consume()
            }

            setOnMouseDragged { event ->
                if(event.button == MouseButton.PRIMARY) event.consume()
            }

            setOnMouseEntered {
                selectionCircle.fill = Color.DARKVIOLET
            }

            setOnMouseExited {
                selectionCircle.fill = Color.WHITE
            }

            setOnDragDetected { event ->
                if(event.button == MouseButton.PRIMARY){
                    val startDragAndDrop = startDragAndDrop(TransferMode.LINK)
                    startDragAndDrop.setContent(mapOf(DragAndDropType.PIN to ""))
                    Core.dragAndDropObject = this@ComponentPin

                    event.consume()
                }
            }

            setOnDragOver { event ->
                if(event.gestureSource != this && event.dragboard.hasContent(DragAndDropType.PIN)){
                    (Core.dragAndDropObject as? ComponentPin)?.let { dragged ->
                        if(dragged.component != this@ComponentPin.component && dragged::class == this@ComponentPin::class){
                            if(dragged.isInputPin() && this@ComponentPin.isOutputPin() || dragged.isOutputPin() && this@ComponentPin.isInputPin()){
                                event.acceptTransferModes(TransferMode.LINK)
                                selectionCircle.fill = Color.DARKVIOLET
                            }
                        }
                    }
                }

                event.consume()
            }

            setOnDragExited {
                selectionCircle.fill = Color.WHITE
            }

            setOnDragDropped { event ->
                val dragboard = event.dragboard
                var success = false

                if(dragboard.hasContent(DragAndDropType.PIN)){
                    success = true
                    (Core.dragAndDropObject as? ComponentPin)?.let { dragged ->
                        if(dragged.component.inputPins.contains(dragged)){
                            dragged.connectWith(this@ComponentPin)
                        }else{
                            this@ComponentPin.connectWith(dragged)
                        }
                    }
                }
                event.isDropCompleted = success
                event.consume()
            }
        }
    }

    fun connectWith(connection: ComponentPin){
        if(this::class == connection::class){
            connections.add(connection)
            component.conneectionsChanged.onNext(Unit)
        }
    }

    protected fun isInputPin(): Boolean {
        return component.inputPins.contains(this)
    }

    protected fun isOutputPin(): Boolean {
        return component.outputPins.contains(this)
    }

    open fun updateUI(){
        if(isInputPin()){
            label.alignment = Pos.CENTER_LEFT
            children.setAll(circle, label)
        }

        if(isOutputPin()){
            label.alignment = Pos.CENTER_RIGHT
            children.setAll(label, circle)
        }
    }

    abstract fun serialize(): JsonObject

    companion object {
        fun deserialize(obj: JsonObject): ComponentPin {
            val cls = Class.forName(obj.get("class").asString).kotlin
            if(cls == ComponentPinFunc::class) {
                return cls.primaryConstructor!!.call(obj.get("name").asString) as ComponentPinFunc
            }
            if(cls == ComponentPinImageFloatBuffer::class) {
                return cls.primaryConstructor!!.call(obj.get("name").asString) as ComponentPinImageFloatBuffer
            }
            if(cls == ComponentPinNumber::class) {
                val value = obj.get("value")
                val numberName = obj.get("numberName")
                val scale = obj.get("scale")
                return if(value != null && numberName != null && scale != null) {
                    cls.primaryConstructor!!.call(obj.get("name").asString, NumberControl(value.asDouble, numberName.asString, scale.asDouble)) as ComponentPinNumber
                } else {
                    cls.primaryConstructor!!.call(obj.get("name").asString, null) as ComponentPinNumber
                }
            }
            if(cls == ComponentPinParticleArray::class) {
                return cls.primaryConstructor!!.call(obj.get("name").asString) as ComponentPinParticleArray
            }
            throw RuntimeException("deserialize error")
        }
    }
}