package spixie

import javafx.scene.control.Label
import javafx.scene.layout.HBox

class ValueLabel(private val name: String) : HBox() {
    private val labelName = Label()
    private val labelValue = Label()
    var value = 0.0
        set(value) {
            field = value
            labelValue.text = field.toString()
        }

    init {
        labelName.text = "$name: "
        labelValue.text = "0.0"
        labelValue.styleClass.add("label-value")
        children.addAll(labelName, labelValue)
    }

    override fun toString(): String {
        return name
    }
}
