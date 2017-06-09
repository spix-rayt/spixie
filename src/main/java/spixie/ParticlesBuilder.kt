package spixie

import java.nio.FloatBuffer
import java.util.*

class ParticlesBuilder {

    private val floats = ArrayList<Float>()

    fun addParticle(x: Float, y: Float, size: Float) {
        floats.add(x)
        floats.add(y)
        floats.add(size)
    }

    fun particlesCount(): Int {
        return floats.size / PARTICLE_FLOAT_SIZE
    }

    fun toFloatBuffer(): FloatBuffer {
        val buffer = FloatBuffer.allocate(floats.size)
        for (aFloat in floats) {
            buffer.put(aFloat!!)
        }
        buffer.rewind()
        return buffer
    }

    companion object {
        val PARTICLE_FLOAT_SIZE = 3
    }
}
