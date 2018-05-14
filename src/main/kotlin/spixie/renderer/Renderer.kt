package spixie.renderer

import java.awt.image.BufferedImage
import java.nio.FloatBuffer

interface Renderer {
    fun render(particlesArray: FloatBuffer): BufferedImage
    fun setSize(width:Int, height:Int)
    fun getSize(): Pair<Int, Int>
}