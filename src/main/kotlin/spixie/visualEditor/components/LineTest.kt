package spixie.visualEditor.components

import org.joml.Vector3f
import spixie.visualEditor.EditorComponent
import spixie.visualEditor.pins.ComponentPinParticleArray
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class LineTest: EditorComponent() {
    private val inParticlesA = ComponentPinParticleArray("ParticlesA")
    private val inParticlesB = ComponentPinParticleArray("ParticlesB")


    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particlesA = inParticlesA.receiveValue()
            val particlesB = inParticlesB.receiveValue()

            val resultArray = particlesA.array.zip(particlesB.array).map { (p1, p2) ->
                val v = Vector3f(p2.matrix.m30() - p1.matrix.m30(), p2.matrix.m31() - p1.matrix.m31(), p2.matrix.m32() - p1.matrix.m32()).normalize()
                (0..(Vector3f(p2.matrix.m30(), p2.matrix.m31(), p2.matrix.m32()).distance(p1.matrix.m30(), p1.matrix.m31(), p1.matrix.m32()).toInt())).map {
                    Particle().apply {
                        matrix.translateLocal(p1.matrix.m30() + v.x * it, p1.matrix.m31() + v.y * it, p1.matrix.m32() + v.z * it)
                    }
                }
            }.flatten()
            ParticleArray(resultArray, resultArray.size.toFloat())
        }
    }

    init {
        inputPins.add(inParticlesA)
        inputPins.add(inParticlesB)
        outputPins.add(outParticles)
        updateUI()
    }
}