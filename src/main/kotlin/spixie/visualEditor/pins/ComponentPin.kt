package spixie.visualEditor.pins

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
import spixie.DragAndDropType
import spixie.dragAndDropObject
import spixie.visualEditor.Component
import spixie.visualEditor.VE_GRID_CELL_SIZE
import spixie.visualEditor.VE_PIN_WIDTH

abstract class ComponentPin(val name: String): HBox() {
    private val backgroundCircle = Circle(VE_GRID_CELL_SIZE /2, VE_GRID_CELL_SIZE /2, 2.0, Color.BLACK)

    private val selectionCircle = Circle(VE_GRID_CELL_SIZE /2, VE_GRID_CELL_SIZE /2, 1.0, Color.WHITE)

    val component: Component
        get() {
            return this.parent.parent as Component
        }

    var typeString = ""

    val circle = StackPane(backgroundCircle, selectionCircle).apply {
        minWidth = VE_GRID_CELL_SIZE
        maxWidth = VE_GRID_CELL_SIZE

        minHeight = VE_GRID_CELL_SIZE
        maxHeight = VE_GRID_CELL_SIZE
    }

    open val connections = mutableSetOf<ComponentPin>()

    protected val label = Label(name.split("~")[0])

    val contextMenu = ContextMenu().apply {
        val unconnectAllItem = MenuItem("Unconnect all").apply {
            setOnAction {
                unconnectAll()
            }
        }
        items.add(unconnectAllItem)
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
                if(event.button == MouseButton.PRIMARY && isInputPin()) {
                    val startDragAndDrop = startDragAndDrop(TransferMode.LINK)
                    startDragAndDrop.setContent(mapOf(DragAndDropType.PIN to ""))
                    dragAndDropObject = this@ComponentPin

                    event.consume()
                }
            }

            setOnDragOver { event ->
                if(event.gestureSource != this && event.dragboard.hasContent(DragAndDropType.PIN)) {
                    (dragAndDropObject as? ComponentPin)?.let { dragged ->
                        if(dragged.component != this@ComponentPin.component && dragged::class == this@ComponentPin::class) {
                            if(dragged.isInputPin() && this@ComponentPin.isOutputPin() || dragged.isOutputPin() && this@ComponentPin.isInputPin()) {
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

                if(dragboard.hasContent(DragAndDropType.PIN)) {
                    success = true
                    (dragAndDropObject as? ComponentPin)?.let { dragged ->
                        if(dragged.component.inputPins.contains(dragged)) {
                            dragged.connectWith(this@ComponentPin)
                        } else {
                            this@ComponentPin.connectWith(dragged)
                        }
                    }
                }
                event.isDropCompleted = success
                event.consume()
            }
        }
    }

    fun connectWith(componentPin: ComponentPin) {
        connections.add(componentPin)
        component.connectionsChanged.onNext(Unit)
    }

    fun disconnectWith(componentPin: ComponentPin) {
        connections.remove(componentPin)
        component.connectionsChanged.onNext(Unit)
    }

    fun unconnectAll() {
        component.disconnectPinRequest.onNext(this)
    }

    protected fun isInputPin(): Boolean {
        return component.inputPins.contains(this)
    }

    protected fun isOutputPin(): Boolean {
        return component.outputPins.contains(this)
    }

    open fun updateUI() {
        if(isInputPin()) {
            label.alignment = Pos.CENTER_LEFT
            children.setAll(circle, label)
        }

        if(isOutputPin()) {
            label.alignment = Pos.CENTER_RIGHT
            children.setAll(label, circle)
        }
    }
}