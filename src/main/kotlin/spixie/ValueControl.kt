package spixie

import io.reactivex.subjects.PublishSubject
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import org.apache.commons.math3.fraction.Fraction

class ValueControl(initial: Double, mul: Double, private val name: String) : HBox() {
    private var startDragValue = Fraction(0)
    private var mul = 1.0
    private val labelName = Label()
    private val labelValue = Label()
    private val textFieldValue = TextField()
    private var dragged = false

    var value = 0.0
        set(value) {
            field = value.coerceIn(min, max)
            labelValue.text = field.toString()
            changes.onNext(field)
        }
    val changes = PublishSubject.create<Double>()

    private var min = Double.NEGATIVE_INFINITY
    fun limitMin(min: Double): ValueControl{
        this.min = min
        return this
    }

    private var max = Double.POSITIVE_INFINITY
    fun limitMax(max:Double): ValueControl{
        this.max = max
        return this
    }

    override fun toString(): String {
        return "ValueControl($name $value)"
    }

    init {
        this.mul = mul
        if(name.isNotEmpty()){
            labelName.text = "$name: "
        }else{
            labelName.text = ""
        }

        labelValue.styleClass.add("label-value")

        labelValue.onMousePressed = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mouseEvent.consume()
                startDragValue = Fraction(value).add(Fraction(this@ValueControl.mul).multiply(mouseEvent.y.toInt()))
                dragged = false
            }
        }

        labelValue.onMouseDragged = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mouseEvent.consume()
                value = startDragValue.subtract(Fraction(this@ValueControl.mul).multiply(mouseEvent.y.toInt())).toDouble()
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
            if (!t1) {
                try {
                    value = java.lang.Double.parseDouble(textFieldValue.text)
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
        value = initial
    }
}
