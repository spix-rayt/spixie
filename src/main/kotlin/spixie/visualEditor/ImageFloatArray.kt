package spixie.visualEditor

import spixie.static.preparePixelsForSave
import spixie.static.toBufferedImage
import java.awt.image.BufferedImage

class ImageFloatArray(val array: FloatArray, val width: Int, val height: Int, val particlesCount: Int = 0) {
    fun toBufferedImage(): BufferedImage{
        return array.preparePixelsForSave(width, height).toBufferedImage(width, height)
    }
}