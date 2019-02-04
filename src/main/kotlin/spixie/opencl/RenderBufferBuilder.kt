package spixie.opencl

import java.nio.FloatBuffer
import java.nio.IntBuffer

class RenderBufferBuilder(particles:Int, val width: Int, val height: Int) {
    private val buffer = FloatBuffer.allocate(particles * PARTICLE_FLOAT_SIZE)

    fun addParticle(x: Float, y: Float, size: Float, red:Float, green:Float, blue:Float, alpha:Float, edge: Float) {
        buffer.put(x)
        buffer.put(y)
        buffer.put(size)
        buffer.put(red)
        buffer.put(green)
        buffer.put(blue)
        buffer.put(alpha)
        buffer.put(edge)
    }

    fun complete(): RenderBuffer {
        buffer.rewind()
        return RenderBuffer(buffer)
    }

    companion object {
        const val PARTICLE_FLOAT_SIZE = 8
    }
}
