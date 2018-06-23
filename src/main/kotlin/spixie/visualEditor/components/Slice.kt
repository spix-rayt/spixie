package spixie.visualEditor.components

import org.apache.commons.lang3.math.Fraction
import spixie.NumberControl
import spixie.static.F_100
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinNumber
import spixie.visualEditor.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray

class Slice: Component(), WithParticlesArrayInput, WithParticlesArrayOutput {
    private val inParticles = ComponentPinParticleArray(this, null, "Particles")
    private val inStart = ComponentPinNumber(this, null, "Start", NumberControl(0.0, 0.1, "").limitMin(0.0).limitMax(100.0))
    private val inEnd = ComponentPinNumber(this, null, "End", NumberControl(100.0, 0.1, "").limitMin(0.0).limitMax(100.0))

    private val outParticles = ComponentPinParticleArray(this, {
        val particles = inParticles.receiveValue()
        val start = Fraction.getFraction(inStart.receiveValue()).divideBy(F_100)
        val end = Fraction.getFraction(inEnd.receiveValue()).divideBy(F_100)

        val from = Fraction.getFraction(particles.array.size.toDouble()).multiplyBy(start).toInt().coerceIn(0..100)
        val to = Fraction.getFraction(particles.array.size.toDouble()).multiplyBy(end).toInt().coerceIn(0..100)

        val resultArray = particles.array.slice(from until to)
        ParticleArray(resultArray, resultArray.size.toFloat())
    }, "Particles")

    init {
        inputPins.add(inParticles)
        inputPins.add(inStart)
        inputPins.add(inEnd)
        outputPins.add(outParticles)
        updateVisual()
    }

    override fun getParticlesArrayInput(): ComponentPinParticleArray {
        return inParticles
    }

    override fun getParticlesArrayOutput(): ComponentPinParticleArray {
        return outParticles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}