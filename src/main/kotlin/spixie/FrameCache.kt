package spixie

import javafx.scene.image.Image
import org.apache.commons.collections4.map.ReferenceMap
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.plugins.jpeg.JPEGImageWriteParam
import kotlin.math.roundToInt

class FrameCache {
    private val map = ReferenceMap<Int, ByteArray>()

    @Synchronized
    fun putImage(bufferedImage: BufferedImage, beats: Double) {
        map[(beats * 48.0).roundToInt()] = bufferedImage.toJPEGByteArray(0.7f)
    }

    @Synchronized
    fun getImageOrNUll(beats: Double): Image? {
        return map[(beats * 48.0).roundToInt()]?.let {
            val byteArrayInputStream = ByteArrayInputStream(it)
            Image(byteArrayInputStream)
        }
    }

    private fun BufferedImage.toJPEGByteArray(quality: Float): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ImageIO.getImageWritersByFormatName("jpg").next().run {
            output = ImageIO.createImageOutputStream(byteArrayOutputStream)
            write(
                null,
                IIOImage(this@toJPEGByteArray, null, null),
                JPEGImageWriteParam(null).apply {
                    compressionMode = JPEGImageWriteParam.MODE_EXPLICIT
                    compressionQuality = quality
                }
            )
        }
        return byteArrayOutputStream.toByteArray()
    }
}