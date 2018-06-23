package spixie.visualEditor.components

import spixie.NumberControl
import spixie.static.convertHueChromaLuminanceToRGB
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinNumber
import spixie.visualEditor.ComponentPinParticleArray

class Color: Component(), WithParticlesArrayInput, WithParticlesArrayOutput {
    private val inParticles = ComponentPinParticleArray(this, null, "Particles")
    private val inHue = ComponentPinNumber(this, null, "Hue", NumberControl(2.0, 0.01, "").limitMin(0.0).limitMax(6.0))
    private val inChroma = ComponentPinNumber(this, null, "Chroma", NumberControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inLuminance = ComponentPinNumber(this, null, "Luminance", NumberControl(1.0, 0.002, "").limitMin(0.0))
    private val inTransparency = ComponentPinNumber(this, null, "Transparency", NumberControl(1.0, 0.001, "").limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPinParticleArray(this, {
        val particles = inParticles.receiveValue()
        val hue = inHue.receiveValue()
        val chroma = inChroma.receiveValue()
        val luminance = inLuminance.receiveValue()
        val transparency = inTransparency.receiveValue().toFloat()

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
    }, "Particles")

    init {
        inputPins.addAll(arrayListOf(inParticles, inHue, inChroma, inLuminance, inTransparency))
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