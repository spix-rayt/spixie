package spixie

import java.nio.FloatBuffer
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class RenderBufferBuilder(val width: Int, val height: Int, val blockSize: Int) {
    private val floatsMap = HashMap<Int, ArrayList<Float>>()

    fun addParticle(x: Float, y: Float, size: Float, red:Float, green:Float, blue:Float, alpha:Float) {
        val pointMinX = x - size - 1f
        val pointMinY = y - size - 1f
        val pointMaxX = x + size + 1f
        val pointMaxY = y + size + 1f

        for (bx in 0..width / blockSize) {
            for (by in 0..height / blockSize) {
                val blockX = bx * blockSize
                val blockY = by * blockSize
                val blockMinX = ((blockX/(width-1.0f))-0.5f)*1000.0f*width/height
                val blockMinY = ((blockY/(height-1.0f))-0.5f)*1000.0f
                val blockMaxX = (((blockX + blockSize)/(width-1.0f))-0.5f)*1000.0f*width/height
                val blockMaxY = (((blockY + blockSize)/(height-1.0f))-0.5f)*1000.0f
                if(!(blockMaxX<pointMinX || pointMaxX<blockMinX || blockMaxY<pointMinY || pointMaxY<blockMinY)){
                    val key = bx*10000+by
                    val arr = floatsMap.getOrPut(key) { ArrayList() }
                    arr.add(x)
                    arr.add(y)
                    arr.add(size)
                    arr.add(red)
                    arr.add(green)
                    arr.add(blue)
                    arr.add(alpha)
                }
            }
        }
    }

    fun toFloatBuffer(x:Int, y:Int): FloatBuffer {
        val floats = floatsMap.getOrElse(x*10000+y) { return FloatBuffer.allocate(0) }
        val buffer = FloatBuffer.allocate(floats.size)
        for (aFloat in floats) {
            buffer.put(aFloat)
        }
        buffer.rewind()
        return buffer
    }

    companion object {
        val PARTICLE_FLOAT_SIZE = 7
    }
}
