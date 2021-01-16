package spixie.visualEditor.components

import spixie.Core
import spixie.opencl.RenderBufferBuilder
import spixie.static.convertHueChromaLuminanceToRGB
import spixie.visualEditor.Component
import spixie.visualEditor.ImageFloatBuffer
import spixie.visualEditor.pins.ComponentPinImageFloatBuffer
import spixie.visualEditor.pins.ComponentPinParticleArray

class Render: Component() {
    private val inParticles = ComponentPinParticleArray("Particles")

    private val outImage = ComponentPinImageFloatBuffer("Image").apply {
        getValue = {
            val particles = inParticles.receiveValue()

            val w = 1920 / Core.arrangementWindow.visualEditor.downscale
            val h = 1080 / Core.arrangementWindow.visualEditor.downscale

            val renderBufferBuilder = RenderBufferBuilder(particles.array.size, w, h)
            particles.array.sortedBy { -it.matrix.m32() }.forEach { particle ->
                if (particle.matrix.m32() >= 40) {
                    val (red, green, blue) = convertHueChromaLuminanceToRGB(
                            if (particle.hasColor()) particle.hue / 6.0 else 2.0 / 6.0,
                            particle.chroma.toDouble(),
                            particle.luminance.toDouble() / 2,
                            false
                    )
                    renderBufferBuilder.addParticle(
                            particle.matrix.m30() / (particle.matrix.m32() / 1000),
                            particle.matrix.m31() / (particle.matrix.m32() / 1000),
                            particle.size / (particle.matrix.m32() / 1000),
                            red.toFloat(),
                            green.toFloat(),
                            blue.toFloat(),
                            particle.transparency,
                            particle.edge
                    )
                }
            }

            throw NotImplementedError("")

//            ImageFloatBuffer(image, w, h, particlesCount = particles.array.size)
        }
    }

    init {
        inputPins.add(inParticles)
        outputPins.add(outImage)
        updateUI()
    }
}