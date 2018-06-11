package spixie.visualEditor.components

import spixie.ValueControl
import spixie.static.convertHueChromaLuminanceToRGB
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ParticleArray

class Color: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inHue = ComponentPin(this, null, "Hue", Double::class.java, ValueControl(2.0, 0.01, "").limitMin(0.0).limitMax(6.0))
    private val inChroma = ComponentPin(this, null, "Chroma", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inLuminance = ComponentPin(this, null, "Luminance", Double::class.java, ValueControl(1.0, 0.002, "").limitMin(0.0))
    private val inTransparency = ComponentPin(this, null, "Transparency", Double::class.java, ValueControl(1.0, 0.001, "").limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(), 0.0f)
        val hue = inHue.receiveValue() ?: 0.0
        val chroma = inChroma.receiveValue() ?: 0.0
        val luminance = inLuminance.receiveValue() ?: 0.0
        val transparency = inTransparency.receiveValue()?.toFloat() ?: 0.0f

        val (r,g,b) = convertHueChromaLuminanceToRGB(hue/6.0, chroma, luminance, false)
        val red = r.toFloat()
        val green = g.toFloat()
        val blue = b.toFloat()

        particles.array.forEach {
            it.red = red
            it.green = green
            it.blue = blue
            it.alpha = transparency
        }
        particles
    }, "Particles", ParticleArray::class.java, null)

    init {
        inputPins.addAll(arrayListOf(inParticles, inHue, inChroma, inLuminance, inTransparency))
        outputPins.add(outParticles)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}