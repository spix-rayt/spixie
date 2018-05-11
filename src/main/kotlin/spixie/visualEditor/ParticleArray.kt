package spixie.visualEditor

import spixie.static.MAGIC
import spixie.static.mix

data class ParticleArray(val array: List<Particle> = arrayListOf()) {
    val hash = array.fold(MAGIC.toLong()) { acc, particle ->
        acc mix particle.spixieHash()
    }
}