package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.ComponentPinFunc
import spixie.visualEditor.ComponentPinNumber

class FuncConst : Func() {
    private val inputFunc = ComponentPinFunc(this, null, "Func")
    private val constValue = ComponentPinNumber(this, null, "Value", NumberControl(0.0, ""))

    private val outFunc = ComponentPinFunc(this, { t->
        val input = inputFunc.receiveValue(t)
        val v = constValue.receiveValue()
        v * input
    }, "Func")

    init {
        inputPins.addAll(arrayListOf(inputFunc, constValue))
        outputPins.add(outFunc)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}