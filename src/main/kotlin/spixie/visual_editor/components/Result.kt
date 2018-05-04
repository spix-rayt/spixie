package spixie.visual_editor.components

import spixie.visual_editor.Component
import spixie.visual_editor.ComponentPin
import spixie.visual_editor.ParticleArray

class Result : Component() {
    val particlesInput = ComponentPin(this, null, "Particles", ParticleArray::class.java)
    init {
        inputPins.add(particlesInput)
        updateVisual()
    }

    fun getParticles(): ParticleArray{
        return particlesInput.receiveValue() ?: ParticleArray()
    }
}