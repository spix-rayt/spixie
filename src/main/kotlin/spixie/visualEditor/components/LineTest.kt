package spixie.visualEditor.components

import org.joml.Vector3f
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class LineTest: Component() {
    private val inParticlesA = ComponentPin(this, null, "ParticlesA", ParticleArray::class.java, null)
    private val inParticlesB = ComponentPin(this, null, "ParticlesB", ParticleArray::class.java, null)


    private val outParticles = ComponentPin(this, {
        val particlesA = inParticlesA.receiveValue() ?: ParticleArray(arrayListOf(Particle()))
        val particlesB = inParticlesB.receiveValue() ?: ParticleArray(arrayListOf(Particle()))

        ParticleArray(
                particlesA.array.zip(particlesB.array).map { (p1,p2) ->
                    val v = Vector3f(p2.matrix.m30() - p1.matrix.m30(), p2.matrix.m31() - p1.matrix.m31(), p2.matrix.m32() - p1.matrix.m32()).normalize()
                    (0..(Vector3f(p2.matrix.m30(), p2.matrix.m31(), p2.matrix.m32()).distance(p1.matrix.m30(), p1.matrix.m31(), p1.matrix.m32()).toInt())).map {
                        Particle().apply {
                            matrix.translateLocal(p1.matrix.m30() + v.x*it, p1.matrix.m31() + v.y*it, p1.matrix.m32() + v.z*it)
                        }
                    }
                }.flatten()
        )
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