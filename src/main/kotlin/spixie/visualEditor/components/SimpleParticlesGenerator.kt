package spixie.visualEditor.components

import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class SimpleParticlesGenerator: Component() {
    private val inCount = ComponentPin(this, null, "Count", Double::class.java, ValueControl(1.0, 0.1, "").limitMin(0.0))


    private val outParticles = ComponentPin(this, {
        val count = inCount.receiveValue() ?: 0.0

        val resultArray = (0 until count.toInt()).map { i ->
            Particle()
        }

        ParticleArray(resultArray, count.toFloat())
    }, "Particles", ParticleArray::class.java, null)

    init {
        inputPins.add(inCount)
        outputPins.add(outParticles)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}