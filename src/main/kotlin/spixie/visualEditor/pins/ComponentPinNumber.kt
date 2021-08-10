package spixie.visualEditor.pins

import javafx.geometry.Pos
import spixie.NumberControl
import spixie.renderManager

class ComponentPinNumber(name: String, val valueControl: NumberControl?): ComponentPin(name) {
    var getValue: (() -> Double)? = null

    fun receiveValue(): Double{
        val values = connections
                .sortedBy { it.editorComponent.layoutY }
                .mapNotNull { (it as? ComponentPinNumber)?.getValue?.invoke() }
        return if(values.isEmpty())
            valueControl?.value ?: 0.0
        else
            if(valueControl != null)
                values.sum().coerceIn(valueControl.min, valueControl.max)
            else
                values.sum()
    }

    init {
        valueControl?.changes?.subscribe {
            renderManager.requestRender()
        }
    }

    override fun updateUI(){
        if(isInputPin()){
            label.alignment = Pos.CENTER_LEFT
            children.setAll(circle, label, valueControl)
        }

        if(isOutputPin()){
            label.alignment = Pos.CENTER_RIGHT
            children.setAll(label, circle)
        }
    }
}