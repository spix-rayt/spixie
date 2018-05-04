package spixie.visual_editor

import spixie.static.magic
import spixie.static.mix

data class ParticleArray(val array: List<Particle> = arrayListOf()) {
    val hash = array.fold(magic.toLong()) { acc, particle ->
        acc mix particle.spixieHash()
    }
}