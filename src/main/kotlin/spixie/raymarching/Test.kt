package spixie.raymarching

import spixie.Core
import spixie.visualEditor.ImageFloatBuffer
import java.io.File
import javax.imageio.ImageIO

class Test {
    fun run() {

        val width = 1920
        val height = 1080

        val image = ImageFloatBuffer(Core.opencl.rayMarchingRender(width, height), width, height, particlesCount = 0)

        ImageIO.write(image.toBufferedImageAndRelease(), "png", File("raymarching.png"))
    }
}



fun main() {
    Test().run()
}