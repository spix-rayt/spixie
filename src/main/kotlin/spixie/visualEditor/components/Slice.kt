package spixie.visualEditor.components

import org.apache.commons.math3.fraction.Fraction
import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class Slice: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inStart = ComponentPin(this, null, "Start",Double::class.java, ValueControl(0.0, 0.1, "").limitMin(0.0).limitMax(100.0))
    private val inEnd = ComponentPin(this, null, "End",Double::class.java, ValueControl(100.0, 0.1, "").limitMin(0.0).limitMax(100.0))

    private val outParticles = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(Particle()))
        val start = Fraction(inStart.receiveValue() ?: 0.0).divide(100)
        val end = Fraction(inEnd.receiveValue() ?: 0.0).divide(100)

        val from = Fraction(particles.array.size).multiply(start).toInt()
        val to = Fraction(particles.array.size).multiply(end).toInt()

        ParticleArray(particles.array.slice(from until to).map {
            it.copy()
        })
    }, "Particles", ParticleArray::class.java, null)

    init {
        inputPins.add(inParticles)
        inputPins.add(inStart)
        inputPins.add(inEnd)
        outputPins.add(outParticles)
        updateVisual()
    }
}