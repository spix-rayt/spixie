package spixie.visualEditor.pins

import javafx.geometry.Pos
import spixie.NumberControl
import spixie.renderManager

class ComponentPinNumber(name: String, val valueControl: NumberControl?): ComponentPin(name) {
    var getValue: (() -> Double)? = null

    fun receiveValue(): Double {
        return receiveValue(null)
    }

    fun receiveValue(t: Double?): Double {
        val numberValues = connections
                .mapNotNull { (it as? ComponentPinNumber)?.getValue?.invoke() }
        val funcValues = if(t != null) {
            connections.mapNotNull { (it as? ComponentPinFunc)?.getValue?.invoke(t) }
        } else {
            emptyList()
        }
        return if(numberValues.isEmpty() && funcValues.isEmpty()) {
            valueControl?.value ?: 0.0
        } else {
            if(valueControl != null) {
                (numberValues.sum() + funcValues.sum()).coerceIn(valueControl.min, valueControl.max)
            } else {
                numberValues.sum() + funcValues.sum()
            }
        }
    }

    init {
        valueControl?.changes?.subscribe {
            renderManager.requestRender()
        }
    }

    override fun updateUI() {
        if(isInputPin()) {
            label.alignment = Pos.CENTER_LEFT
            children.setAll(circle, label, valueControl)
        }

        if(isOutputPin()) {
            label.alignment = Pos.CENTER_RIGHT
            children.setAll(label, circle)
        }
    }
}