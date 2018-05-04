package spixie

import javafx.scene.control.Label
import javafx.scene.layout.HBox

class ValueLabel(private val name: String) : HBox() {
    private val labelName = Label()
    private val labelValue = Label()
    val value = Value(0.0)

    init {
        labelName.text = name + ": "
        labelValue.text = "0.0"
        labelValue.styleClass.add("label-value")
        children.addAll(labelName, labelValue)

        value.changes.subscribe { newValue ->
            labelValue.text = newValue.toString()
        }
    }

    override fun toString(): String {
        return name
    }
}
