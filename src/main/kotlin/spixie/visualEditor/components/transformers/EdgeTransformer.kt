package spixie.visualEditor.components.transformers

import spixie.static.linearInterpolate
import spixie.static.perlinInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray

class EdgeTransformer: ParticleArrayTransformer(0.0, 0.001, 0.0, 1.0) {
    override fun transform(particles: ParticleArray): ParticleArray {
        when(parameterMode.value!!){
            Mode.Simple -> {
                val v = inputValue.receiveValue().toFloat()
                particles.array.forEach { it.edge = v }
            }
            Mode.Linear -> {
                val first = inputFirst.receiveValue()
                val last = inputLast.receiveValue()
                particles.array.forEachIndexed { index, particle ->
                    val t = if(particles.decimalSize>1) index.toDouble()/(particles.decimalSize-1.0) else 0.0
                    particle.edge = linearInterpolate(first, last, t).toFloat()
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
                    particle.edge = (finalRand*(max - min)+min).toFloat()
                }
            }
        }
        return particles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}