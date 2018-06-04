package spixie.visualEditor.components

import spixie.Main
import spixie.renderer.RenderBufferBuilder
import spixie.visualEditor.*

class Render: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)

    private val outImage = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(Particle()))

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
                        particle.alpha
                )
            }
        }
        val w = 1920/Main.arrangementWindow.visualEditor.downscale
        val h = 1080/Main.arrangementWindow.visualEditor.downscale
        Main.renderManager.renderer.setSize(w, h)
        val image = Main.renderManager.renderer.render(renderBufferBuilder.complete(), false)

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