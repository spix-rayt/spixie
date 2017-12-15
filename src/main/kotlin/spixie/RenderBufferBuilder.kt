package spixie

import java.nio.FloatBuffer
import java.util.*

class RenderBufferBuilder {
    private val floats = ArrayList<Float>()

    fun addParticle(x: Float, y: Float, size: Float, red:Float, green:Float, blue:Float, alpha:Float) {
        floats.add(x)
        floats.add(y)
        floats.add(size)
        floats.add(red)
        floats.add(green)
        floats.add(blue)
        floats.add(alpha)
    }

    fun particlesCount(): Int {
        return floats.size / PARTICLE_FLOAT_SIZE
    }

    fun toFloatBuffer(): FloatBuffer {
        val buffer = FloatBuffer.allocate(floats.size)
        for (aFloat in floats) {
            buffer.put(aFloat)
        }
        buffer.rewind()
        return buffer
    }

    fun toFloatBuffer(blockX: Int, blockY: Int, blockSize: Int, width: Int, height: Int): FloatBuffer{
        val filteredFloats = ArrayList<Float>()
        val blockMinX = ((blockX/(width-1.0f))-0.5f)*1000.0f*width/height
        val blockMinY = ((blockY/(height-1.0f))-0.5f)*1000.0f
        val blockMaxX = (((blockX + blockSize)/(width-1.0f))-0.5f)*1000.0f*width/height
        val blockMaxY = (((blockY + blockSize)/(height-1.0f))-0.5f)*1000.0f

        for (i in 0 until particlesCount()){
            val pointX = floats[i * PARTICLE_FLOAT_SIZE]
            val pointY = floats[i * PARTICLE_FLOAT_SIZE + 1]
            val pointSize = floats[i * PARTICLE_FLOAT_SIZE + 2]
            val pointMinX = pointX - pointSize
            val pointMinY = pointY - pointSize
            val pointMaxX = pointX + pointSize
            val pointMaxY = pointY + pointSize

            if(!(blockMaxX<pointMinX || pointMaxX<blockMinX || blockMaxY<pointMinY || pointMaxY<blockMinY)){
                for (j in 0 until PARTICLE_FLOAT_SIZE){
                    filteredFloats.add(floats[i * PARTICLE_FLOAT_SIZE + j])
                }
            }
        }
        val buffer = FloatBuffer.allocate(filteredFloats.size)
        for (aFloat in filteredFloats) {
            buffer.put(aFloat)
        }
        buffer.rewind()
        return buffer
    }

    companion object {
        val PARTICLE_FLOAT_SIZE = 7
    }
}
