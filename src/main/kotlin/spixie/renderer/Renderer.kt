package spixie.renderer

import java.nio.FloatBuffer

interface Renderer {
    fun render(particlesArray: FloatBuffer, depth: Boolean): FloatArray
    fun setSize(width:Int, height:Int)
    fun getSize(): Pair<Int, Int>
}