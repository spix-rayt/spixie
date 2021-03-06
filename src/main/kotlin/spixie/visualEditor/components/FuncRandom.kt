package spixie.visualEditor.components

import spixie.NumberControl
import spixie.static.linearInterpolate
import spixie.static.perlinInterpolate
import spixie.static.rand
import spixie.visualEditor.pins.ComponentPinFunc
import spixie.visualEditor.pins.ComponentPinNumber

class FuncRandom : Func() {
    private val inputFunc = ComponentPinFunc("Func")

    private val startValue = ComponentPinNumber("Start", NumberControl(0.0, ""))

    private val endValue = ComponentPinNumber("End", NumberControl(0.0, ""))

    private val offsetValue = ComponentPinNumber("Offset", NumberControl(0.0, ""))

    private val stretchValue = ComponentPinNumber("Stretch", NumberControl(0.0, ""))

    private val seedValue = ComponentPinNumber("Seed", NumberControl(0.0, ""))

    private val outFunc = ComponentPinFunc("Func").apply {
        getValue = { t ->
            val input = inputFunc.receiveValue(t)
            val start = startValue.receiveValue()
            val end = endValue.receiveValue()
            val offset = offsetValue.receiveValue()
            val stretch = stretchValue.receiveValue()
            val seed = seedValue.receiveValue()


            val i = ((t + offset) * stretch)
            val leftRandom = rand(0, 0, 0, 0, seed.toLong(), i.toLong()).toDouble()
            val rightRandom = rand(0, 0, 0, 0, seed.toLong(), i.toLong() + 1L).toDouble()
            val rand = perlinInterpolate(leftRandom, rightRandom, i % 1)
            val leftRandom2 = rand(0, 0, 0, 0, seed.toLong() + 1, i.toLong()).toDouble()
            val rightRandom2 = rand(0, 0, 0, 0, seed.toLong() + 1, i.toLong() + 1L).toDouble()
            val rand2 = perlinInterpolate(leftRandom2, rightRandom2, i % 1)
            val finalRand = linearInterpolate(rand, rand2, seed % 1)
            (start + (end - start) * finalRand) * input
        }
    }

    init {
        inputPins.addAll(arrayListOf(inputFunc, startValue, endValue, offsetValue, stretchValue, seedValue))
        outputPins.add(outFunc)
        updateUI()
    }
}