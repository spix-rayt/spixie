package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ParticleArray

class Result : Component() {
    private val particlesInput = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    init {
        inputPins.add(particlesInput)
        updateVisual()
    }

    fun getParticles(): ParticleArray{
        return particlesInput.receiveValue() ?: ParticleArray()
    }
}