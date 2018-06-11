package spixie.opencl

import java.nio.FloatBuffer

class RenderBufferBuilder(particles:Int) {
    private val buffer = FloatBuffer.allocate(particles * PARTICLE_FLOAT_SIZE)

    fun addParticle(x: Float, y: Float, size: Float, red:Float, green:Float, blue:Float, alpha:Float, edge: Float, glow: Float) {
        buffer.put(x)
        buffer.put(y)
        buffer.put(size)
        buffer.put(red)
        buffer.put(green)
        buffer.put(blue)
        buffer.put(alpha)
        buffer.put(edge)
        buffer.put(glow)
    }

    fun complete(): FloatBuffer {
        buffer.rewind()
        return buffer
    }

    companion object {
        const val PARTICLE_FLOAT_SIZE = 9
    }
}
