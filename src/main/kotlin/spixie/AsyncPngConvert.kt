package spixie

import spixie.static.toPNGByteArray
import java.awt.image.BufferedImage

class AsyncPngConvert(bufferedImage:BufferedImage) {
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