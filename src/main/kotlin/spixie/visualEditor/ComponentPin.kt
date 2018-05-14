package spixie.visualEditor

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
import spixie.Main
import spixie.ValueControl
import spixie.static.DragAndDropType

class ComponentPin<T>(val component: Component, val getValue: (() -> T)?, name: String, private val type: Class<T>, val valueControl: ValueControl?): HBox() {
    private val backgroundCircle = Circle(16.0, 16.0, 5.0, Color.BLACK)
    private val selectionCircle = Circle(16.0, 16.0, 4.0, Color.WHITE)
    val circle = StackPane(backgroundCircle, selectionCircle).apply {
        minWidth = 32.0
        maxWidth = 32.0

        minHeight = 32.0
        maxHeight = 32.0
    }
    var connection: ComponentPin<*>? = null
        set(value) {
            field = value
            component.conneectionsChanged.onNext(Unit)
        }

    private val label = Label(name)

    init {
        label.apply {
            minWidth = 128.0 - 32.0
            maxWidth = 128.0 - 32.0

            minHeight = 32.0
            maxHeight = 32.0
        }

        valueControl?.changes?.subscribe { Main.renderManager.requestRender() }

        setOnContextMenuRequested { event->
            ContextMenu(
                    MenuItem("Unconnect all").apply {
                        setOnAction {
                            component.disconnectPinRequest.onNext(this@ComponentPin)
                        }
                    }
            ).show(this, event.screenX, event.screenY)
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
                    Main.dragAndDropObject = this@ComponentPin

                    event.consume()
                }
            }

            setOnDragOver { event ->
                if(event.gestureSource != this && event.dragboard.hasContent(DragAndDropType.PIN)){
                    (Main.dragAndDropObject as? ComponentPin<*>)?.let { dragged ->
                        if(dragged.component != this@ComponentPin.component && dragged.type == this@ComponentPin.type){
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
                    (Main.dragAndDropObject as? ComponentPin<*>)?.let { dragged ->
                        if(dragged.component.inputPins.contains(dragged)){
                            dragged.connection = this@ComponentPin
                        }else{
                            this@ComponentPin.connection = dragged
                        }
                    }
                }
                event.isDropCompleted = success
                event.consume()
            }
        }
    }

    private fun isInputPin(): Boolean {
        return component.inputPins.contains(this)
    }

    private fun isOutputPin(): Boolean {
        return component.outputPins.contains(this)
    }

    fun receiveValue(): T? {
        val valueFromConnection = connection?.getValue?.invoke() as? T
        return if(valueFromConnection == null && type == Double::class.java && valueControl != null){
            valueControl.value as? T
        }else{
            valueFromConnection
        }
    }

    fun updateUI(){
        if(isInputPin()){
            label.alignment = Pos.CENTER_LEFT
            if(valueControl == null){
                children.setAll(circle, label)
            }else{
                children.setAll(circle, label, valueControl)
            }
        }

        if(isOutputPin()){
            label.alignment = Pos.CENTER_RIGHT
            children.setAll(label, circle)
        }
    }
}