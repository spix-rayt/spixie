package spixie.visualEditor.components.transformers

import spixie.static.linearInterpolate
import spixie.static.perlinInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray

class HueTransformer: ParticleArrayTransformer(2.0, 0.01, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) {
    override fun transform(particles: ParticleArray): ParticleArray {
        when(parameterMode.value!!){
            Mode.Simple -> {
                val v = inputValue.receiveValue().toFloat()
                particles.array.forEach { it.hue = v }
            }
            Mode.Linear -> {
                val first = inputFirst.receiveValue()
                val last = inputLast.receiveValue()
                particles.array.forEachIndexed { index, particle ->
                    val t = if(particles.decimalSize>1) index.toDouble()/(particles.decimalSize-1.0) else 0.0
                    particle.hue = linearInterpolate(first, last, t).toFloat()
                }
            }
            Mode.Random -> {
                val min = inputMin.receiveValue()
                val max = inputMax.receiveValue().coerceAtLeast(min)
                val offset = inputOffset.receiveValue()
                val stretch = inputStretch.receiveValue()
                val seed = inputSeed.receiveValue()
                particles.array.forEachIndexed { index, particle ->
                    val i = ((index+offset) / stretch)
                    val leftRandom  = rand(0, 0, 0, 0, seed.toLong(), i.toLong()).toDouble()
                    val rightRandom = rand(0, 0, 0, 0, seed.toLong(), i.toLong()+1L).toDouble()
                    val rand = perlinInterpolate(leftRandom, rightRandom, i%1)
                    val leftRandom2  = rand(0, 0, 0, 0, seed.toLong()+1, i.toLong()).toDouble()
                    val rightRandom2 = rand(0, 0, 0, 0, seed.toLong()+1, i.toLong()+1L).toDouble()
                    val rand2 = perlinInterpolate(leftRandom2, rightRandom2, i%1)
                    val finalRand = linearInterpolate(rand, rand2, seed%1)
                    particle.hue = (finalRand*(max - min)+min).toFloat()
                }
            }
        }
        return particles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}