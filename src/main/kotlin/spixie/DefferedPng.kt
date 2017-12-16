package spixie

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class DefferedPng(bufferedImage:BufferedImage) {
    val thread:Thread
    var value:ByteArray? = null
    init {
        thread = Thread(Runnable {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream)
            value = byteArrayOutputStream.toByteArray()
        })
        thread.start()
    }

    fun get():ByteArray? {
        if(value == null){
            thread.join()
        }
        return value
    }
}