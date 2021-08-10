package spixie.visualEditor.components

import spixie.openCLApi
import spixie.render.Splat
import spixie.static.convertHueChromaLuminanceToRGB
import spixie.visualEditor.EditorComponent
import spixie.render.ImageCLBuffer
import spixie.render.RenderParameters
import spixie.visualEditor.pins.ComponentPinParticleArray

class RenderComponent: EditorComponent() {
    private val inParticles = ComponentPinParticleArray("Particles")

    fun invoke(imageCLBuffer: ImageCLBuffer, samplesPerPixel: Int) {
        val particles = inParticles.receiveValue()

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

        openCLApi.renderSplats(imageCLBuffer, splats, RenderParameters(samplesPerPixel, 0, 0, 1.0f, 3.0f))
    }

    init {
        inputPins.add(inParticles)
        updateUI()
    }
}