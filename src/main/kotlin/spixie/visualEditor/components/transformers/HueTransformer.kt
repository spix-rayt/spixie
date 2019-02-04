package spixie.visualEditor.components.transformers

import spixie.static.linearInterpolate
import spixie.static.perlinInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray

class HueTransformer: ParticleArrayTransformer() {
    override fun transform(particles: ParticleArray): ParticleArray {
        particles.array.forEachIndexed { index, particle ->
            val t = if (particles.decimalSize > 1) index.toDouble() / (particles.decimalSize - 1.0) else 0.0
            particle.hue = inputFunc.receiveValue(t).toFloat()
        }
        return particles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}