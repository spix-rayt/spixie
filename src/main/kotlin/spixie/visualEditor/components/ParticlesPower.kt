package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.*

class ParticlesPower: Component() {
    private val inParticles = ComponentPinParticleArray(this, null, "Particles")
    private val inPower = ComponentPinNumber(this, null, "Power", NumberControl(1.0, 0.1, "").limitMin(0.0))

    private val outParticles = ComponentPinParticleArray(this, {
        val particles = inParticles.receiveValue()
        val power = inPower.receiveValue().toInt()

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
    }, "Particles")

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