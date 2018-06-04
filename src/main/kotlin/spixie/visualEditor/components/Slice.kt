package spixie.visualEditor.components

import org.apache.commons.lang3.math.Fraction
import spixie.ValueControl
import spixie.static.F_100
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
        val start = Fraction.getFraction(inStart.receiveValue() ?: 0.0).divideBy(F_100)
        val end = Fraction.getFraction(inEnd.receiveValue() ?: 0.0).divideBy(F_100)

        val from = Fraction.getFraction(particles.array.size.toDouble()).multiplyBy(start).toInt().coerceIn(0..100)
        val to = Fraction.getFraction(particles.array.size.toDouble()).multiplyBy(end).toInt().coerceIn(0..100)

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

    companion object {
        const val serialVersionUID = 0L
    }
}