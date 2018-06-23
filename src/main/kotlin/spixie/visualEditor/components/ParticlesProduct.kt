package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinParticleArray
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class ParticlesProduct: Component() {
    private val inParticlesA = ComponentPinParticleArray(this, null, "ParticlesA")
    private val inParticlesB = ComponentPinParticleArray(this, null, "ParticlesB")

    private val outParticles = ComponentPinParticleArray(this, {
        val particlesA = inParticlesA.receiveValue()
        val particlesB = inParticlesB.receiveValue()


        val resultArray = particlesB.array.flatMap { pb ->
            particlesA.array.map { pa ->
                Particle().apply {
                    pb.matrix.mul(pa.matrix, matrix)
                    this.size = pa.size*pb.size
                }
            }
        }
        ParticleArray(resultArray, resultArray.size.toFloat())
    }, "Particles")

    init {
        inputPins.add(inParticlesA)
        inputPins.add(inParticlesB)
        outputPins.add(outParticles)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}