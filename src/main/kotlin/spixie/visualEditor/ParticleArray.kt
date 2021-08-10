package spixie.visualEditor

import spixie.static.MAGIC
import spixie.static.mix

class ParticleArray(val array: List<Particle>, val decimalSize: Float) {
    val hash = array.fold(MAGIC.toLong()) { acc, particle ->
        acc mix particle.spixieHash()
    }

    inline fun forEachWithGradient(f: (t: Double, particle: Particle) -> Unit) {
        array.forEachIndexed { index, particle ->
            val t = if (decimalSize > 1) index.toDouble() / (decimalSize - 1.0) else 0.0
            f(t, particle)
        }
    }

    override fun toString(): String {
        return "Particles(${array.size})"
    }
}