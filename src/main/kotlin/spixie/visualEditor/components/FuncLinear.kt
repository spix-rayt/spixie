package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.pins.ComponentPinFunc
import spixie.visualEditor.pins.ComponentPinNumber

class FuncLinear : Func() {
    private val inputFunc = ComponentPinFunc("Func")

    private val startValue = ComponentPinNumber("Start", NumberControl(0.0, ""))

    private val endValue = ComponentPinNumber("End", NumberControl(0.0, ""))

    private val outFunc = ComponentPinFunc("Func").apply {
        getValue = { t ->
            val input = inputFunc.receiveValue(t)
            val start = startValue.receiveValue()
            val end = endValue.receiveValue()
            (start + (end - start) * t) * input
        }
    }

    init {
        inputPins.addAll(arrayListOf(inputFunc, startValue, endValue))
        outputPins.add(outFunc)
        updateUI()
    }
}