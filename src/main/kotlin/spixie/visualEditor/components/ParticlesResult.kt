package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray

class ParticlesResult : Component() {
    private val inputParticles by lazyPinFromListOrCreate(0) { ComponentPinParticleArray("Result") }

    override fun creationInit() {
        inputPins.add(inputParticles)
    }

    override fun configInit() {
        updateUI()
    }

    fun getParticles(): ParticleArray{
        return inputParticles.receiveValue()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}