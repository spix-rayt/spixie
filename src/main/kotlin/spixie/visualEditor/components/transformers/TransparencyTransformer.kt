package spixie.visualEditor.components.transformers

import spixie.visualEditor.ParticleArray

class TransparencyTransformer: ParticleArrayTransformer() {
    override fun transform(particles: ParticleArray): ParticleArray {
        particles.array.forEachIndexed { index, particle ->
            val t = if (particles.decimalSize > 1) index.toDouble() / (particles.decimalSize - 1.0) else 0.0
            particle.transparency = inputFunc.receiveValue(t).toFloat().coerceIn(0.0f, 1.0f)
        }
        return particles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}