package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class Color: Component() {
    private val inParticles by lazyPinFromListOrCreate(0) { ComponentPinParticleArray("Particles") }

    private val inHue by lazyPinFromListOrCreate(1) { ComponentPinNumber("Hue", NumberControl(2.0, "", -300.0)) }

    private val inChroma by lazyPinFromListOrCreate(2) { ComponentPinNumber("Chroma", NumberControl(1.0, "", -300.0).limitMin(0.0).limitMax(1.0)) }

    private val inLuminance by lazyPinFromListOrCreate(3) { ComponentPinNumber("Luminance", NumberControl(1.0, "", -300.0).limitMin(0.0)) }

    private val inTransparency by lazyPinFromListOrCreate(4) { ComponentPinNumber("Transparency", NumberControl(1.0, "", -300.0).limitMin(0.0).limitMax(1.0)) }

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

    override fun creationInit() {
        inputPins.addAll(arrayListOf(inParticles, inHue, inChroma, inLuminance, inTransparency))
    }

    override fun configInit() {
        outputPins.add(outParticles)
        updateUI()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}