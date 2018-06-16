package spixie.visualEditor.components

import spixie.static.linearInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray
import kotlin.math.roundToLong

class EdgeTransformer: ParticleArrayTransformer(1.0, 0.001, 0.0, 1.0) {
    override fun transform(particles: ParticleArray): ParticleArray {
        when(parameterMode.value){
            Mode.Simple -> {
                val v = (inputSimpleValue.receiveValue() ?: 0.0).toFloat()
                particles.array.forEach { it.edge = v }
            }
            Mode.Linear -> {
                val first = (inputLinearFirst.receiveValue() ?: 0.0)
                val last = (inputLinearLast.receiveValue() ?: 0.0)
                particles.array.forEachIndexed { index, particle ->
                    val t = if(particles.decimalSize>1) index.toDouble()/(particles.decimalSize-1.0) else 0.0
                    particle.edge = linearInterpolate(first, last, t).toFloat()
                }
            }
            Mode.Random -> {
                val min = (inputRandomMin.receiveValue() ?: 0.0)
                val max = (inputRandomMax.receiveValue() ?: 0.0).coerceAtLeast(min)
                val seed = (inputRandomSeed.receiveValue() ?: 0.0).roundToLong()
                particles.array.forEachIndexed { index, particle ->
                    particle.edge = (rand(0, 0, 0, 0, seed, index.toLong())*(max - min)+min).toFloat()
                }
            }
        }
        return particles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}