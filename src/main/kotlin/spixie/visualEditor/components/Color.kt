package spixie.visualEditor.components

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import spixie.NumberControl
import spixie.gson
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class Color: Component() {
    private val inParticles = ComponentPinParticleArray("Particles")

    private val inHue = ComponentPinNumber("Hue", NumberControl(2.0, "", -300.0))

    private val inChroma = ComponentPinNumber("Chroma", NumberControl(1.0, "", -300.0).limitMin(0.0).limitMax(1.0))

    private val inLuminance = ComponentPinNumber("Luminance", NumberControl(1.0, "", -300.0).limitMin(0.0))

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particles = inParticles.receiveValue()

            particles.forEachWithGradient { t, particle ->
                particle.hue = inHue.receiveValue(t).toFloat()
                particle.chroma = inChroma.receiveValue(t).toFloat()
                particle.luminance = inLuminance.receiveValue(t).toFloat()
            }
            particles
        }
    }

    init {
        inputPins.addAll(arrayListOf(inParticles, inHue, inChroma, inLuminance))
        outputPins.add(outParticles)
        updateUI()
    }
}