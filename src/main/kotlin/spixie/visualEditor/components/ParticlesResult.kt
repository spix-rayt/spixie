package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray

class ParticlesResult : Component() {
    private val inputParticles = ComponentPinParticleArray("Result")

    init {
        inputPins.add(inputParticles)
        updateUI()
    }

    fun getParticles(): ParticleArray{
        return inputParticles.receiveValue()
    }
}