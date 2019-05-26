package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinParticleArray
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class ParticlesProduct: Component() {
    private val inParticlesA by lazyPinFromListOrCreate(0) { ComponentPinParticleArray("ParticlesA") }
    private val inParticlesB by lazyPinFromListOrCreate(1) { ComponentPinParticleArray("ParticlesB") }

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particlesA = inParticlesA.receiveValue()
            val particlesB = inParticlesB.receiveValue()


            val resultArray = particlesB.array.flatMap { pb ->
                particlesA.array.map { pa ->
                    Particle().apply {
                        pb.matrix.mul(pa.matrix, matrix)

                        if (pa.hasColor()) {
                            if (pb.hasColor()) {
                                this.hue = pa.hue + pb.hue
                            } else {
                                this.hue = pa.hue
                            }
                        } else {
                            if (pb.hasColor()) {
                                this.hue = pb.hue
                            }
                        }
                        this.chroma = pa.chroma * pb.chroma
                        this.luminance = pa.luminance * pb.luminance
                        this.transparency = pa.transparency * pb.transparency
                        this.size = pa.size * pb.size
                    }
                }
            }
            ParticleArray(resultArray, resultArray.size.toFloat())
        }
    }

    override fun creationInit() {
        inputPins.add(inParticlesA)
        inputPins.add(inParticlesB)
    }

    override fun configInit() {
        outputPins.add(outParticles)
        updateUI()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}