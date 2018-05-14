package spixie.visualEditor.components

import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class Color: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inRed = ComponentPin(this, null, "Red", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inGreen = ComponentPin(this, null, "Green", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inBlue = ComponentPin(this, null, "Blue", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inAlpha = ComponentPin(this, null, "Alpha", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(Particle()))
        val red = inRed.receiveValue() ?: 0.0
        val green = inGreen.receiveValue() ?: 0.0
        val blue = inBlue.receiveValue() ?: 0.0
        val alpha = inAlpha.receiveValue() ?: 0.0

        ParticleArray(particles.array.map {
            it.copy().apply {
                this.red = red.toFloat()
                this.green = green.toFloat()
                this.blue = blue.toFloat()
                this.alpha = alpha.toFloat()
            }
        })
    }, "Particles", ParticleArray::class.java, null)

    init {
        inputPins.add(inParticles)
        inputPins.add(inRed)
        inputPins.add(inGreen)
        inputPins.add(inBlue)
        inputPins.add(inAlpha)
        outputPins.add(outParticles)
        updateVisual()
    }
}