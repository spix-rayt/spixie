package spixie.visualEditor.components

import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class Color: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inHue = ComponentPin(this, null, "Hue", Double::class.java, ValueControl(1.0, 0.1, "").limitMin(0.0).limitMax(6.0))
    private val inSaturation = ComponentPin(this, null, "Saturation", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))
    private val inLightness = ComponentPin(this, null, "Lightness", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(2.0))
    private val inOpacity = ComponentPin(this, null, "Opacity", Double::class.java, ValueControl(1.0, 0.01, "").limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(Particle()))
        val hue = inHue.receiveValue()?.coerceIn(0.0..6.0)?.div(6.0) ?: 0.0
        val saturation = inSaturation.receiveValue()?.coerceIn(0.0..1.0) ?: 0.0
        val lightness = inLightness.receiveValue()?.coerceIn(0.0..2.0)?.div(2.0) ?: 0.0
        val opacity = inOpacity.receiveValue() ?: 0.0

        ParticleArray(particles.array.map {
            it.copy().apply {
                val q = if(lightness<0.5) lightness * (1 + saturation) else lightness + saturation - lightness * saturation
                val p = 2 * lightness - q

                this.red = hue2rgb(p, q, hue + 1/3.0).toFloat()
                this.green = hue2rgb(p, q, hue).toFloat()
                this.blue = hue2rgb(p, q, hue - 1/3.0).toFloat()
                this.alpha = opacity.toFloat()
            }
        })
    }, "Particles", ParticleArray::class.java, null)

    private fun hue2rgb(p: Double, q: Double, t:Double): Double {
        val h = when{
            t<0 -> t+1
            t>1 -> t-1
            else -> t
        }
        return when{
            h*6.0 < 1 -> p+(q-p)*6*h
            h*2.0 < 1 -> q
            h*3.0 < 2 -> p + (q-p)*(2/3.0-h)*6
            else -> p
        }
    }

    init {
        inputPins.add(inParticles)
        inputPins.add(inHue)
        inputPins.add(inSaturation)
        inputPins.add(inLightness)
        inputPins.add(inOpacity)
        outputPins.add(outParticles)
        updateVisual()
    }
}