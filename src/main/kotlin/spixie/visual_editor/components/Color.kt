package spixie.visual_editor.components

import spixie.ValueControl
import spixie.visual_editor.Component
import spixie.visual_editor.ComponentPin
import spixie.visual_editor.ParticleArray

class Color: Component() {
    val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    val inRed = ComponentPin(this, null, "Red", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    val inGreen = ComponentPin(this, null, "Green", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    val inBlue = ComponentPin(this, null, "Blue", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    val inAlpha = ComponentPin(this, null, "Alpha", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))

    val outParticles = ComponentPin(this, {
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