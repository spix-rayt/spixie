package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.ComponentPinFunc
import spixie.visualEditor.ComponentPinNumber
import kotlin.math.sin

class FuncSin : Func() {
    private val inputFunc = ComponentPinFunc(this, null, "Func")

    private val startValue = ComponentPinNumber(this, null, "Start", NumberControl(0.0, ""))

    private val endValue = ComponentPinNumber(this, null, "End", NumberControl(0.0, ""))

    private val shiftValue = ComponentPinNumber(this, null, "Shift", NumberControl(0.0, ""))

    private val frequencyValue = ComponentPinNumber(this, null, "Period", NumberControl(0.0, ""))

    private val outFunc = ComponentPinFunc(this, { t->
        val input = inputFunc.receiveValue(t)
        val start = startValue.receiveValue()
        val end = endValue.receiveValue()
        val shift = shiftValue.receiveValue()
        val frequency = frequencyValue.receiveValue()
        (start + ((sin(shift + t*frequency) + 1.0)*0.5) * (end - start)) * input
    }, "Func")

    init {
        inputPins.addAll(arrayListOf(inputFunc, startValue, endValue, shiftValue, frequencyValue))
        outputPins.add(outFunc)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}