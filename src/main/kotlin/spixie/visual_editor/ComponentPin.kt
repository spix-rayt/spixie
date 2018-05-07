package spixie.visual_editor

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
import spixie.static.DragAndDropType

class ComponentPin<T>(val component: Component, val getValue: (() -> T)?, name: String, val type: Class<T>): HBox() {
    val backgroundCircle = Circle(16.0, 16.0, 5.0, Color.BLACK)
    val selectionCircle = Circle(16.0, 16.0, 4.0, Color.WHITE)
    val circle = StackPane(backgroundCircle, selectionCircle).apply {
        minWidth = 32.0
        maxWidth = 32.0

        minHeight = 32.0
        maxHeight = 32.0
    }

    val label = Label(name)

    init {
        label.apply {
            minWidth = 128.0 - 32.0
            maxWidth = 128.0 - 32.0

            minHeight = 32.0
            maxHeight = 32.0
        }

        setOnContextMenuRequested { event->
            ContextMenu(
                    MenuItem("Unconnect all").apply {
                        setOnAction {
                            val iterator = Main.arrangementWindow.visualEditor.inputToOutputConnection.iterator()
                            for(entry in iterator){
                                if(entry.key == this@ComponentPin || entry.value == this@ComponentPin){
                                    iterator.remove()
                                }
                            }
                            Main.arrangementWindow.visualEditor.reconnectPins()
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
                    startDragAndDrop.setContent(mapOf(DragAndDropType.INTERNALOBJECT to ""))
                    Main.internalObject = this@ComponentPin

                    event.consume()
                }
            }

            setOnDragOver { event ->
                if(event.gestureSource != this && event.dragboard.hasContent(DragAndDropType.INTERNALOBJECT)){
                    (Main.internalObject as? ComponentPin<*>)?.let { dragged ->
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

                if(dragboard.hasContent(DragAndDropType.INTERNALOBJECT)){
                    success = true
                    (Main.internalObject as? ComponentPin<*>)?.let { dragged ->
                        if(dragged.component.inputPins.contains(dragged)){
                            Main.arrangementWindow.visualEditor.inputToOutputConnection[dragged] = this@ComponentPin
                        }else{
                            Main.arrangementWindow.visualEditor.inputToOutputConnection[this@ComponentPin] = dragged
                        }

                        Main.arrangementWindow.visualEditor.reconnectPins()
                    }
                }
                event.isDropCompleted = success
                event.consume()
            }
        }
    }

    fun isInputPin(): Boolean {
        return component.inputPins.contains(this)
    }

    fun isOutputPin(): Boolean {
        return component.outputPins.contains(this)
    }

    fun receiveValue(): T? {
        return Main.arrangementWindow.visualEditor.inputToOutputConnection[this]?.getValue?.invoke() as? T
    }

    fun relocateNodes(){
        if(isInputPin()){
            label.alignment = Pos.CENTER_LEFT
            children.setAll(circle, label)
        }

        if(isOutputPin()){
            label.alignment = Pos.CENTER_RIGHT
            children.setAll(label, circle)
        }
    }
}