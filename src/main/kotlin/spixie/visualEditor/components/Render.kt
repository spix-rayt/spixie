package spixie.visualEditor.components

import spixie.Main
import spixie.opencl.RenderBufferBuilder
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinImageFloatBuffer
import spixie.visualEditor.ComponentPinParticleArray
import spixie.visualEditor.ImageFloatBuffer

class Render: Component(), WithParticlesArrayInput {
    private val inParticles = ComponentPinParticleArray(this, null, "Particles")

    private val outImage = ComponentPinImageFloatBuffer(this, {
        val particles = inParticles.receiveValue()

        val renderBufferBuilder = RenderBufferBuilder(particles.array.size)
        particles.array.sortedBy { -it.matrix.m32() }.forEach { particle ->
            if(particle.matrix.m32()>=40){
                renderBufferBuilder.addParticle(
                        particle.matrix.m30()/(particle.matrix.m32()/1000),
                        particle.matrix.m31()/(particle.matrix.m32()/1000),
                        particle.size/(particle.matrix.m32()/1000),
                        particle.red,
                        particle.green,
                        particle.blue,
                        particle.alpha,
                        particle.edge,
                        particle.glow
                )
            }
        }
        val w = 1920/Main.arrangementWindow.visualEditor.downscale
        val h = 1080/Main.arrangementWindow.visualEditor.downscale
        val image = Main.opencl.render(renderBufferBuilder.complete(), w, h)

        ImageFloatBuffer(image, w, h, particlesCount = particles.array.size)
    }, "Image")

    init {
        inputPins.add(inParticles)
        outputPins.add(outImage)
        updateVisual()
    }

    override fun getParticlesArrayInput(): ComponentPinParticleArray {
        return inParticles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}