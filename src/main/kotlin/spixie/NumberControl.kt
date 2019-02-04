package spixie

import io.reactivex.subjects.PublishSubject
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import org.apache.commons.lang3.math.Fraction
import java.awt.Robot
import kotlin.math.roundToInt

class NumberControl(initial: Double, private val name: String) : HBox() {
    private var mousePressedScreenX = 0.0

    private var mousePressedScreenY = 0.0

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

    var min = Double.NEGATIVE_INFINITY
        private set

    fun limitMin(min: Double): NumberControl{
        this.min = min
        return this
    }

    var max = Double.POSITIVE_INFINITY
        private set

    fun limitMax(max:Double): NumberControl{
        this.max = max
        return this
    }

    override fun toString(): String {
        return "NumberControl($name $value)"
    }

    init {
        if(name.isNotEmpty()){
            labelName.text = "$name: "
        }else{
            labelName.text = ""
        }

        labelValue.styleClass.add("label-value")

        labelValue.onMousePressed = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mousePressedScreenX = mouseEvent.screenX
                mousePressedScreenY = mouseEvent.screenY
                mouseEvent.consume()
                dragged = false
            }
        }

        labelValue.onMouseDragged = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mouseEvent.consume()
                if(mouseEvent.screenX != mousePressedScreenX || mouseEvent.screenY != mousePressedScreenY) {
                    Robot().mouseMove(mousePressedScreenX.toInt(), mousePressedScreenY.toInt())
                }
                value = Fraction.getFraction(value).add(Fraction.getFraction(mousePressedScreenY.toInt() - mouseEvent.screenY.toInt(), 1).multiplyBy(Fraction.getFraction(Main.dragMultiplier))).toDouble()
                labelValue.cursor = Cursor.NONE
                dragged = true
            }
        }

        labelValue.onMouseReleased = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (!dragged) {
                    mouseEvent.consume()
                    children.remove(labelValue)
                    children.addAll(textFieldValue)
                    textFieldValue.text = value.toString()
                    textFieldValue.requestFocus()
                    textFieldValue.selectAll()
                }else{
                    labelValue.cursor = Cursor.V_RESIZE
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
