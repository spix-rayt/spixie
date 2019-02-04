package spixie.visualEditor

import javafx.geometry.Pos
import spixie.Main
import spixie.NumberControl

class ComponentPinNumber(component: Component, val getValue: (() -> Double)?, name: String, val valueControl: NumberControl?): ComponentPin(component, name) {
    fun receiveValue(): Double{
        val allConnections = (connections + imaginaryConnections).toSet()
        val values = allConnections
                .sortedBy { it.component.layoutY }
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
        valueControl?.changes?.subscribe { Main.renderManager.requestRender() }
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