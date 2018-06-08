package spixie.visualEditor.components

import spixie.Main
import spixie.ValueControl
import spixie.renderer.RenderBufferBuilder
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ImageFloatArray
import spixie.visualEditor.ParticleArray
import kotlin.math.abs

class RenderDepth: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inZeroRadius = ComponentPin(this, null, "ZeroRadius", Double::class.java, ValueControl(0.0, 5.0, "").limitMin(0.0))
    private val inWidth = ComponentPin(this, null, "Width", Double::class.java, ValueControl(0.0, 5.0, "").limitMin(0.0))

    private val outImage = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(), 0.0f)
        val zeroRadius = inZeroRadius.receiveValue() ?: 0.0
        val width = inWidth.receiveValue() ?: 0.0

        val renderBufferBuilder = RenderBufferBuilder(particles.array.size)
        particles.array.sortedBy { -it.matrix.m32() }.forEach { particle ->
            if(particle.matrix.m32()>=40){
                val d = Math.sqrt((particle.matrix.m30() * particle.matrix.m30() + particle.matrix.m31() * particle.matrix.m31() + particle.matrix.m32() * particle.matrix.m32()).toDouble())
                val g = (1.0 - (abs(d-zeroRadius)/width).coerceIn(0.0..1.0)).toFloat()
                renderBufferBuilder.addParticle(
                        particle.matrix.m30()/(particle.matrix.m32()/1000),
                        particle.matrix.m31()/(particle.matrix.m32()/1000),
                        particle.size/(particle.matrix.m32()/1000),
                        g,
                        g,
                        g,
                        1.0f
                )
            }
        }
        val w = 1920/Main.arrangementWindow.visualEditor.downscale
        val h = 1080/Main.arrangementWindow.visualEditor.downscale
        Main.renderManager.renderer.setSize(w, h)
        val image = Main.renderManager.renderer.render(renderBufferBuilder.complete(), true)

        ImageFloatArray(image, w, h, particlesCount = particles.array.size)
    }, "Image", ImageFloatArray::class.java, null)

    init {
        inputPins.add(inParticles)
        inputPins.add(inZeroRadius)
        inputPins.add(inWidth)
        outputPins.add(outImage)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}