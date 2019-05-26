package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.pins.ComponentPinFunc
import spixie.visualEditor.pins.ComponentPinNumber
import kotlin.math.sin

class FuncSin : Func() {
    private val inputFunc by lazyPinFromListOrCreate(0) { ComponentPinFunc("Func") }

    private val startValue by lazyPinFromListOrCreate(1) { ComponentPinNumber("Start", NumberControl(0.0, "")) }

    private val endValue by lazyPinFromListOrCreate(2) { ComponentPinNumber("End", NumberControl(0.0, "")) }

    private val shiftValue by lazyPinFromListOrCreate(3) { ComponentPinNumber("Shift", NumberControl(0.0, "")) }

    private val frequencyValue by lazyPinFromListOrCreate(4) { ComponentPinNumber("Period", NumberControl(0.0, "")) }

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

    override fun creationInit() {
        inputPins.addAll(arrayListOf(inputFunc, startValue, endValue, shiftValue, frequencyValue))
    }

    override fun configInit() {
        outputPins.add(outFunc)
        updateUI()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}