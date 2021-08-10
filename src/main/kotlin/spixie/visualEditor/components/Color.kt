package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.EditorComponent
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class Color: EditorComponent() {
    private val inParticles = ComponentPinParticleArray("Particles")

    private val inHue = ComponentPinNumber("Hue", NumberControl(2.0, "", -300.0))

    private val inChroma = ComponentPinNumber("Chroma", NumberControl(1.0, "", -300.0).limitMin(0.0).limitMax(1.0))

    private val inLuminance = ComponentPinNumber("Luminance", NumberControl(1.0, "", -300.0).limitMin(0.0))

    private val inTransparency = ComponentPinNumber("Transparency", NumberControl(1.0, "", -300.0).limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
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
        }
    }

    init {
        inputPins.addAll(arrayListOf(inParticles, inHue, inChroma, inLuminance, inTransparency))
        outputPins.add(outParticles)
        updateUI()
    }
}