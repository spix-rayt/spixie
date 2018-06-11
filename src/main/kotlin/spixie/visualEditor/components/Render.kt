package spixie.visualEditor.components

import spixie.Main
import spixie.opencl.RenderBufferBuilder
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ImageFloatArray
import spixie.visualEditor.ParticleArray

class Render: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)

    private val outImage = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(), 0.0f)

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
        Main.opencl.setSize(w, h)
        val image = Main.opencl.render(renderBufferBuilder.complete())

        ImageFloatArray(image, w, h, particlesCount = particles.array.size)
    }, "Image", ImageFloatArray::class.java, null)

    init {
        inputPins.add(inParticles)
        outputPins.add(outImage)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}