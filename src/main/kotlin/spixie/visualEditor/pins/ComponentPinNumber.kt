package spixie.visualEditor.pins

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import javafx.geometry.Pos
import spixie.Core
import spixie.NumberControl
import spixie.visualEditor.Component
import java.io.ObjectInput
import java.io.ObjectOutput

class ComponentPinNumber(name: String, val valueControl: NumberControl?): ComponentPin(name) {
    var getValue: (() -> Double)? = null

    fun receiveValue(): Double{
        val values = connections
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
        valueControl?.changes?.subscribe { Core.renderManager.requestRender() }
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

    override fun serialize(): JsonObject {
        val obj = JsonObject()
        obj.add("class", JsonPrimitive(this::class.qualifiedName))
        obj.add("name", JsonPrimitive(name))
        if(valueControl != null) {
            obj.add("value", JsonPrimitive(valueControl.value))
            obj.add("numberName", JsonPrimitive(valueControl.name))
            obj.add("scale", JsonPrimitive(valueControl.numberLineScale))
        }
        return obj
    }
}