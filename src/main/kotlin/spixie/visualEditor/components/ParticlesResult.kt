package spixie.visualEditor.components

import spixie.visualEditor.EditorComponent
import spixie.visualEditor.pins.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray

class ParticlesResult : EditorComponent() {
    private val inputParticles = ComponentPinParticleArray("Result")

    init {
        inputPins.add(inputParticles)
        updateUI()
    }

    fun getParticles(): ParticleArray{
        return inputParticles.receiveValue()
    }
}