package spixie

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class DeferredPng(bufferedImage:BufferedImage) {
    private val thread:Thread
    var value:ByteArray? = null
    init {
        thread = Thread(Runnable { value = bufferedImage.toPNGByteArray() })
        thread.start()
    }

    fun get():ByteArray? {
        if(value == null){
            thread.join()
        }
        return value
    }
}