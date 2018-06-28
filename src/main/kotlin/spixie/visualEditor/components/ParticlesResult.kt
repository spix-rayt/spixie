package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray

class ParticlesResult : Component() {
    private val inputParticles = ComponentPinParticleArray(this, null, "Result")
    init {
        inputPins.add(inputParticles)
        updateVisual()
    }

    fun getParticles(): ParticleArray{
        return inputParticles.receiveValue()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}