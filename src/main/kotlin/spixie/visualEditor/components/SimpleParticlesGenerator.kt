package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.*
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class SimpleParticlesGenerator: Component() {
    private val inCount by lazyPinFromListOrCreate(0) { ComponentPinNumber("Count", NumberControl(1.0, "").limitMin(0.0).limitMax(100000.0)) }

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val count = inCount.receiveValue()

            ParticleArray(Array(count.toInt()) { Particle() }.toList(), count.toFloat())
        }
    }

    override fun creationInit() {
        inputPins.add(inCount)
    }

    override fun configInit() {
        outputPins.add(outParticles)
        updateUI()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}