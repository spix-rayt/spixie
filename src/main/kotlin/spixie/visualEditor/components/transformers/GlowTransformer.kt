package spixie.visualEditor.components.transformers

import spixie.static.linearInterpolate
import spixie.static.perlinInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray
import kotlin.math.roundToLong

class GlowTransformer: ParticleArrayTransformer(1.0, 0.01, 1.0, 100.0) {
    override fun transform(particles: ParticleArray): ParticleArray {
        when(parameterMode.value!!){
            Mode.Simple -> {
                val v = inputSimpleValue.receiveValue().toFloat()
                particles.array.forEach { it.glow = v }
            }
            Mode.Linear -> {
                val first = inputLinearFirst.receiveValue()
                val last = inputLinearLast.receiveValue()
                particles.array.forEachIndexed { index, particle ->
                    val t = if(particles.decimalSize>1) index.toDouble()/(particles.decimalSize-1.0) else 0.0
                    particle.glow = linearInterpolate(first, last, t).toFloat()
                }
            }
            Mode.Random -> {
                val min = inputRandomMin.receiveValue()
                val max = inputRandomMax.receiveValue().coerceAtLeast(min)
                val stretch = inputRandomStretch.receiveValue()
                val seed = inputRandomSeed.receiveValue().roundToLong()
                particles.array.forEachIndexed { index, particle ->
                    val i = (index / stretch)
                    val leftRandom  = rand(0, 0, 0, 0, seed, i.toLong()).toDouble()
                    val rightRandom = rand(0, 0, 0, 0, seed, i.toLong()+1L).toDouble()
                    val rand = perlinInterpolate(leftRandom, rightRandom, i%1)
                    particle.glow = (rand*(max - min)+min).toFloat()
                }
            }
        }
        return particles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}