package spixie.visualEditor.components

import spixie.ValueControl
import spixie.static.convertHueChromaLuminanceToRGB
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class Color: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inHue = ComponentPin(this, null, "Hue", Double::class.java, ValueControl(2.0, 0.01, "").limitMin(0.0).limitMax(6.0))
    private val inChroma = ComponentPin(this, null, "Chroma", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inLuminance = ComponentPin(this, null, "Luminance", Double::class.java, ValueControl(1.0, 0.01, ""))
    private val inTransparency = ComponentPin(this, null, "Transparency", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(Particle()))
        val hue = inHue.receiveValue()?.coerceIn(0.0..6.0) ?: 0.0
        val chroma = inChroma.receiveValue()?.coerceIn(0.0..1.0) ?: 0.0
        val luminance = inLuminance.receiveValue() ?: 0.0
        val transparency = inTransparency.receiveValue()?.coerceIn(0.0..1.0) ?: 0.0

        val (r,g,b) = convertHueChromaLuminanceToRGB(hue/6.0, chroma, luminance, false)

        ParticleArray(particles.array.map {
            it.copy().apply {
                this.red = Math.pow(r, 2.2).toFloat()
                this.green = Math.pow(g, 2.2).toFloat()
                this.blue = Math.pow(b, 2.2).toFloat()
                this.alpha = transparency.toFloat()
            }
        })
    }, "Particles", ParticleArray::class.java, null)

    init {
        inputPins.add(inParticles)
        inputPins.add(inHue)
        inputPins.add(inChroma)
        inputPins.add(inLuminance)
        inputPins.add(inTransparency)
        outputPins.add(outParticles)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}