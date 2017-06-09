package spixie

import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import org.apache.commons.math3.fraction.Fraction

class Value(initial: Double, mul: Double, private val name: String) : HBox() {
    var fraction = Fraction(0)
        private set
    private var startDragValue = Fraction(0)
    private var mul = 1.0
    private val subscribers = ArrayList<ValueChanger>()
    private val labelName = Label()
    private val labelValue = Label()
    private val textFieldValue = TextField()
    private var dragged = false

    init {
        this.mul = mul
        labelName.text = name + ": "
        set(initial)
        labelValue.styleClass.add("label-value")

        labelValue.onMousePressed = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                startDragValue = fraction.add(Fraction(this@Value.mul).multiply(mouseEvent.y.toInt()))
                dragged = false
            }
            if (mouseEvent.button == MouseButton.SECONDARY) {
                val parent = parent
                if (parent is Multiplier) {
                    parent.addGraph(this@Value)
                }
            }
        }

        labelValue.onMouseDragged = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                set(startDragValue.subtract(Fraction(this@Value.mul).multiply(mouseEvent.y.toInt())))
                dragged = true
            }
        }

        labelValue.onMouseReleased = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (!dragged) {
                    textFieldValue.text = this@Value.fraction.toDouble().toString()
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
            if (keyEvent.code == KeyCode.ENTER) {
                children.remove(textFieldValue)
            }
        }



        children.addAll(labelName, labelValue)
    }

    fun set(value: Double) {
        set(Fraction(value))
    }

    fun set(value: Fraction) {
        if (value.compareTo(Fraction.ZERO) < 0) {
            this.fraction = Fraction(0)
        } else {
            this.fraction = value
        }
        labelValue.text = this.fraction.toDouble().toString()
        for (valueChanger in subscribers) {
            valueChanger.updateOutValue()
        }
    }

    fun get(): Double {
        return fraction.toDouble()
    }

    private val itemForCheckbox = Item(this)
    fun item(): Item {
        return itemForCheckbox
    }

    class Item(val value: Value) {

        fun subscribeChanger(valueChanger: ValueChanger) {
            if (!value.subscribers.contains(valueChanger)) {
                value.subscribers.add(valueChanger)
            }
        }

        fun unsubscribeChanger(valueChanger: ValueChanger) {
            value.subscribers.remove(valueChanger)
        }

        fun checkCycle(valueChanger: ValueChanger): Boolean {
            return valueChanger.valueToBeChanged.checkCycleInternal(arrayOf(this), null, null)
        }

        fun checkCycle(fakeValueChanger: ValueChanger, fakeItem: Item): Boolean {
            return checkCycleInternal(arrayOf<Item?>(), fakeValueChanger, fakeItem)
        }

        private fun checkCycleInternal(values: Array<Item?>, fakeValueChanger: ValueChanger?, fakeItem: Item?): Boolean {
            for (item in values) {
                if (item === this) {
                    return false
                }
            }
            val newValues = arrayOfNulls<Item>(values.size + 1)
            for (i in values.indices) {
                newValues[i] = values[i]
            }
            newValues[newValues.size - 1] = this
            for (subscriber in value.subscribers) {
                val valueToBeChanged: Item?
                if (subscriber === fakeValueChanger) {
                    valueToBeChanged = fakeItem
                } else {
                    valueToBeChanged = subscriber.valueToBeChanged
                }
                if (valueToBeChanged != null) {
                    if (!valueToBeChanged.checkCycleInternal(newValues, fakeValueChanger, fakeItem)) {
                        return false
                    }
                }
            }

            return true
        }

        override fun toString(): String {
            return value.name
        }
    }

    companion object{
        val EMPTY:Value = Value(0.0,0.0,"")
    }
}
