package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.ComponentPinFunc
import spixie.visualEditor.ComponentPinNumber

class FuncLinear : Func() {
    private val inputFunc = ComponentPinFunc(this, null, "Func")

    private val startValue = ComponentPinNumber(this, null, "Start", NumberControl(0.0, ""))

    private val endValue = ComponentPinNumber(this, null, "End", NumberControl(0.0, ""))

    private val outFunc = ComponentPinFunc(this, { t->
        val input = inputFunc.receiveValue(t)
        val start = startValue.receiveValue()
        val end = endValue.receiveValue()
        (start + (end - start) * t) * input
    }, "Func")

    init {
        inputPins.addAll(arrayListOf(inputFunc, startValue, endValue))
        outputPins.add(outFunc)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}