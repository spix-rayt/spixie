package spixie.visualEditor.components

import spixie.Core
import spixie.raymarching.Splat
import spixie.raymarching.SplatRenderer
import spixie.static.convertHueChromaLuminanceToRGB
import spixie.static.roundUp
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinImageFloatBuffer
import spixie.visualEditor.pins.ComponentPinParticleArray

class Render: Component() {
    private val inParticles = ComponentPinParticleArray("Particles")

    private val outImage = ComponentPinImageFloatBuffer("Image").apply {
        getValue = {
            val particles = inParticles.receiveValue()

            val w = (1920 / Core.arrangementWindow.visualEditor.downscale).roundUp(2)
            val h = (1080 / Core.arrangementWindow.visualEditor.downscale).roundUp(2)

            val splats = particles.array.map { particle ->
                val (red, green, blue) = convertHueChromaLuminanceToRGB(
                    if (particle.hasColor()) particle.hue / 6.0 else 2.0 / 6.0,
                    particle.chroma.toDouble(),
                    particle.luminance.toDouble() / 2,
                    false
                )

                Splat().apply {
                    x = particle.matrix.m30()
                    y = particle.matrix.m31()
                    z = particle.matrix.m32()
                    size = particle.size
                    r = red.toFloat()
                    g = green.toFloat()
                    b = blue.toFloat()
                }
            }

            SplatRenderer.render(splats, w, h)
        }
    }

    init {
        inputPins.add(inParticles)
        outputPins.add(outImage)
        updateUI()
    }
}