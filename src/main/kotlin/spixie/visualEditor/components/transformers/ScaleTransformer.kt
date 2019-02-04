package spixie.visualEditor.components.transformers

import spixie.static.linearInterpolate
import spixie.static.perlinInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray

class ScaleTransformer: ParticleArrayTransformer() {
    override fun transform(particles: ParticleArray): ParticleArray {
        particles.array.forEachIndexed { index, particle ->
            val t = if (particles.decimalSize > 1) index.toDouble() / (particles.decimalSize - 1.0) else 0.0
            particle.matrix.scaleLocal(inputFunc.receiveValue(t).toFloat().coerceAtLeast(0.0f))
        }
        return particles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}