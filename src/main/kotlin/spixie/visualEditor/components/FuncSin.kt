package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.pins.ComponentPinFunc
import spixie.visualEditor.pins.ComponentPinNumber
import kotlin.math.sin

class FuncSin : Func() {
    private val inputFunc = ComponentPinFunc("Func")

    private val startValue = ComponentPinNumber("Start", NumberControl(0.0, ""))

    private val endValue = ComponentPinNumber("End", NumberControl(0.0, ""))

    private val shiftValue = ComponentPinNumber("Shift", NumberControl(0.0, ""))

    private val frequencyValue = ComponentPinNumber("Period", NumberControl(0.0, ""))

    private val outFunc = ComponentPinFunc("Func").apply {
        getValue = { t ->
            val input = inputFunc.receiveValue(t)
            val start = startValue.receiveValue()
            val end = endValue.receiveValue()
            val shift = shiftValue.receiveValue()
            val frequency = frequencyValue.receiveValue()
            (start + ((sin(shift + t * frequency) + 1.0) * 0.5) * (end - start)) * input
        }
    }

    init {
        inputPins.addAll(arrayListOf(inputFunc, startValue, endValue, shiftValue, frequencyValue))
        outputPins.add(outFunc)
        updateUI()
    }
}