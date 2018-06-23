package spixie.visualEditor.components.transformers

import spixie.static.linearInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray
import kotlin.math.roundToLong

class ScaleTransformer: ParticleArrayTransformer(1.0, 0.1, 0.0, Double.POSITIVE_INFINITY) {
    override fun transform(particles: ParticleArray): ParticleArray {
        when(parameterMode.value!!){
            Mode.Simple -> {
                val v = inputSimpleValue.receiveValue().toFloat()
                particles.array.forEach { it.matrix.scaleLocal(v) }
            }
            Mode.Linear -> {
                val first = inputLinearFirst.receiveValue()
                val last = inputLinearLast.receiveValue()
                particles.array.forEachIndexed { index, particle ->
                    val t = if(particles.decimalSize>1) index.toDouble()/(particles.decimalSize-1.0) else 0.0
                    particle.matrix.scaleLocal(linearInterpolate(first, last, t).toFloat())
                }
            }
            Mode.Random -> {
                val min = inputRandomMin.receiveValue()
                val max = inputRandomMax.receiveValue().coerceAtLeast(min)
                val seed = inputRandomSeed.receiveValue().roundToLong()
                particles.array.forEachIndexed { index, particle ->
                    particle.matrix.scaleLocal((rand(0, 0, 0, 0, seed, index.toLong())*(max - min)+min).toFloat())
                }
            }
        }
        return particles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}