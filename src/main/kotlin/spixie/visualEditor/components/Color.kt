package spixie.visualEditor.components

import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ParticleArray

class Color: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inRed = ComponentPin(this, null, "Red", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inGreen = ComponentPin(this, null, "Green", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inBlue = ComponentPin(this, null, "Blue", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inAlpha = ComponentPin(this, null, "Alpha", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPin(this, {
        val particles = inParticles.receiveValue()
        val red = inRed.receiveValue()
        val green = inGreen.receiveValue()
        val blue = inBlue.receiveValue()
        val alpha = inAlpha.receiveValue()

        if (particles != null && red != null && green != null && blue != null && alpha != null) {
            ParticleArray(particles.array.map {
                it.copy().apply {
                    this.red = red.toFloat()
                    this.green = green.toFloat()
                    this.blue = blue.toFloat()
                    this.alpha = alpha.toFloat()
                }
            })
        } else {
            ParticleArray()
        }
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