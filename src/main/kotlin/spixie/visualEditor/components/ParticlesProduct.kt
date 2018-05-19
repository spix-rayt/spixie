package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class ParticlesProduct: Component() {
    private val inParticlesA = ComponentPin(this, null, "ParticlesA", ParticleArray::class.java, null)
    private val inParticlesB = ComponentPin(this, null, "ParticlesB", ParticleArray::class.java, null)

    private val outParticles = ComponentPin(this, {
        val particlesA = inParticlesA.receiveValue() ?: ParticleArray(arrayListOf())
        val particlesB = inParticlesB.receiveValue() ?: ParticleArray(arrayListOf())

        ParticleArray(
                particlesB.array.flatMap { pb ->
                    particlesA.array.map { pa ->
                        Particle().apply {
                            matrix.set(pb.matrix.mul(pa.matrix))
                            this.size = pa.size*pb.size
                        }
                    }
                }
        )
    }, "Particles", ParticleArray::class.java, null)

    init {
        inputPins.add(inParticlesA)
        inputPins.add(inParticlesB)
        outputPins.add(outParticles)
        updateVisual()
    }
}