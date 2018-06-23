package spixie.visualEditor.components

import org.joml.Vector3f
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinParticleArray
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class LineTest: Component() {
    private val inParticlesA = ComponentPinParticleArray(this, null, "ParticlesA")
    private val inParticlesB = ComponentPinParticleArray(this, null, "ParticlesB")


    private val outParticles = ComponentPinParticleArray(this, {
        val particlesA = inParticlesA.receiveValue()
        val particlesB = inParticlesB.receiveValue()

        val resultArray = particlesA.array.zip(particlesB.array).map { (p1,p2) ->
            val v = Vector3f(p2.matrix.m30() - p1.matrix.m30(), p2.matrix.m31() - p1.matrix.m31(), p2.matrix.m32() - p1.matrix.m32()).normalize()
            (0..(Vector3f(p2.matrix.m30(), p2.matrix.m31(), p2.matrix.m32()).distance(p1.matrix.m30(), p1.matrix.m31(), p1.matrix.m32()).toInt())).map {
                Particle().apply {
                    matrix.translateLocal(p1.matrix.m30() + v.x*it, p1.matrix.m31() + v.y*it, p1.matrix.m32() + v.z*it)
                }
            }
        }.flatten()
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