package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class ParticlesProduct: Component() {
    private val inParticlesA = ComponentPin(this, null, "ParticlesA", ParticleArray::class.java, null)
    private val inParticlesB = ComponentPin(this, null, "ParticlesB", ParticleArray::class.java, null)

    private val outParticles = ComponentPin(this, {
        val particlesA = inParticlesA.receiveValue() ?: ParticleArray(arrayListOf(), 0.0f)
        val particlesB = inParticlesB.receiveValue() ?: ParticleArray(arrayListOf(), 0.0f)


        val resultArray = particlesB.array.flatMap { pb ->
            particlesA.array.map { pa ->
                Particle().apply {
                    pb.matrix.mul(pa.matrix, matrix)
                    this.size = pa.size*pb.size
                }
            }
        }
        ParticleArray(resultArray, resultArray.size.toFloat())
    }, "Particles", ParticleArray::class.java, null)

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