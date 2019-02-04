package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinNumber
import spixie.visualEditor.ComponentPinParticleArray

class Color: Component(), WithParticlesArrayInput, WithParticlesArrayOutput {
    private val inParticles = ComponentPinParticleArray(this, null, "Particles")

    private val inHue = ComponentPinNumber(this, null, "Hue", NumberControl(2.0, ""))

    private val inChroma = ComponentPinNumber(this, null, "Chroma", NumberControl(1.0, "").limitMin(0.0).limitMax(1.0))

    private val inLuminance = ComponentPinNumber(this, null, "Luminance", NumberControl(1.0, "").limitMin(0.0))

    private val inTransparency = ComponentPinNumber(this, null, "Transparency", NumberControl(1.0, "").limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPinParticleArray(this, {
        val particles = inParticles.receiveValue()
        val hue = inHue.receiveValue().toFloat()
        val chroma = inChroma.receiveValue().toFloat()
        val luminance = inLuminance.receiveValue().toFloat()
        val transparency = inTransparency.receiveValue().toFloat()

        particles.array.forEach {
            it.hue = hue
            it.chroma = chroma
            it.luminance = luminance
            it.transparency = transparency
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