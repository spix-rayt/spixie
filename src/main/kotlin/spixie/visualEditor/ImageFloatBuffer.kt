package spixie.visualEditor

import com.jogamp.opencl.CLBuffer
import spixie.Core
import spixie.static.toBufferedImage
import java.awt.image.BufferedImage
import java.nio.FloatBuffer

class ImageFloatBuffer(val buffer: CLBuffer<FloatBuffer>, val width: Int, val height: Int, val particlesCount: Int = 0){
    fun toBufferedImageAndRelease(): BufferedImage {
        val bufferedImage = Core.opencl.brightPixelsToWhite(buffer, width, height).toBufferedImage(width, height)
        buffer.release()
        return bufferedImage
    }
}