package spixie.visualEditor.components

import org.apache.commons.lang3.math.Fraction
import spixie.NumberControl
import spixie.static.F_100
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray

class Slice: Component() {
    private val inParticles = ComponentPinParticleArray("Particles")
    private val inStart = ComponentPinNumber("Start", NumberControl(0.0, "").limitMin(0.0).limitMax(100.0))
    private val inEnd = ComponentPinNumber("End", NumberControl(100.0, "").limitMin(0.0).limitMax(100.0))

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particles = inParticles.receiveValue()
            val start = Fraction.getFraction(inStart.receiveValue()).divideBy(F_100)
            val end = Fraction.getFraction(inEnd.receiveValue()).divideBy(F_100)

            val from = Fraction.getFraction(particles.array.size.toDouble()).multiplyBy(start).toInt().coerceIn(0..100)
            val to = Fraction.getFraction(particles.array.size.toDouble()).multiplyBy(end).toInt().coerceIn(0..100)

            val resultArray = particles.array.slice(from until to)
            ParticleArray(resultArray, resultArray.size.toFloat())
        }
    }

    init {
        inputPins.add(inParticles)
        inputPins.add(inStart)
        inputPins.add(inEnd)
        outputPins.add(outParticles)
        updateUI()
    }
}