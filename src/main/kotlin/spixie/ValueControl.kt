package spixie

import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.*
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import org.apache.commons.math3.fraction.Fraction
import spixie.components.Graph

class ValueControl(initial: Double, mul: Double, private val name: String) : HBox() {
    private var startDragValue = Fraction(0)
    private var mul = 1.0
    private val labelName = Label()
    private val labelValue = Label()
    private val textFieldValue = TextField()
    private var dragged = false

    val value = Value<Double>(initial)

    private var min = 0.0
    fun limitMin(min: Double): ValueControl{
        this.min = min
        return this
    }

    private var max = Double.MAX_VALUE
    fun limitMax(max:Double): ValueControl{
        this.max = max
        return this
    }

    var onInputOutputConnected: (Any, Any) -> Unit = { _, _ ->  }

    fun set(value: Double) {
        when {
            value < min -> this.value.value = min
            value > max -> this.value.value = max
            else -> this.value.value = value
        }
    }

    override fun toString(): String {
        return name
    }

    init {
        this.mul = mul
        labelName.text = name + ": "
        labelValue.styleClass.add("label-value")

        labelValue.onMousePressed = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mouseEvent.consume()
                startDragValue = Fraction(value.value).add(Fraction(this@ValueControl.mul).multiply(mouseEvent.y.toInt()))
                dragged = false
            }
        }

        labelValue.onMouseDragged = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mouseEvent.consume()
                set(startDragValue.subtract(Fraction(this@ValueControl.mul).multiply(mouseEvent.y.toInt())).toDouble())
                dragged = true
            }
        }

        labelValue.onMouseReleased = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (!dragged) {
                    mouseEvent.consume()
                    children.remove(labelValue)
                    children.addAll(textFieldValue)
                    textFieldValue.requestFocus()
                    textFieldValue.selectAll()
                }
            }
        }

        textFieldValue.focusedProperty().addListener { _, _, t1 ->
            if (!t1!!) {
                try {
                    set(java.lang.Double.parseDouble(textFieldValue.text))
                } catch (e: NumberFormatException) {
                }

                children.remove(textFieldValue)
                children.addAll(labelValue)
            }
        }

        textFieldValue.onKeyPressed = EventHandler<KeyEvent> { keyEvent ->
            if(keyEvent.code == KeyCode.ESCAPE || keyEvent.code == KeyCode.ENTER){
                children.remove(textFieldValue)
                keyEvent.consume()
            }
        }
        children.addAll(labelName, labelValue)

        setOnDragOver { event ->
            if(event.dragboard.hasContent(DragAndDropType.INTERNALOBJECT)){
                event.acceptTransferModes(TransferMode.LINK)
            }
            event.consume()
        }

        setOnDragDropped { event ->
            val dragboard = event.dragboard
            var success = false
            if(dragboard.hasContent(DragAndDropType.INTERNALOBJECT)){
                onInputOutputConnected(Main.internalObject, this)
                val obj = Main.internalObject
                if(obj is Graph){
                    obj.graphWindow.value.input = value
                }
                success = true
            }
            event.isDropCompleted = success
            event.consume()
        }

        setOnDragDetected { event ->
            if(event.button == MouseButton.PRIMARY){
                if(event.isControlDown){
                    val startDragAndDrop = this.startDragAndDrop(javafx.scene.input.TransferMode.LINK);
                    val content = ClipboardContent()
                    content.put(DragAndDropType.INTERNALOBJECT, "void")
                    startDragAndDrop.setContent(content)
                    Main.internalObject = this
                    event.consume()
                }
            }
        }

        value.onChanged { newValue ->
            labelValue.text = newValue.toString()
        }
        set(initial)
    }
}
