package spixie.visualEditor.components

import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class ParticlesPower: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inPower = ComponentPin(this, null, "Power", Double::class.java, ValueControl(1.0, 0.1, "").limitMin(0.0))

    private val outParticles = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(), 0.0f)
        val power = inPower.receiveValue()?.toInt() ?: 0

        var particlesB = particles.array

        (2..power).forEach{
            particlesB = particlesB.flatMap { pb ->
                particles.array.map { pa ->
                    Particle().apply {
                        pb.matrix.mul(pa.matrix, matrix)
                        this.size = pa.size*pb.size
                    }
                }
            }
        }

        ParticleArray(particlesB, particlesB.size.toFloat())
    }, "Particles", ParticleArray::class.java, null)

    init {
        inputPins.add(inParticles)
        inputPins.add(inPower)
        outputPins.add(outParticles)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}