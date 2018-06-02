package spixie.visualEditor.components

import spixie.Main
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ImageFloatArray
import java.nio.ByteBuffer

class FocusBlur: Component() {
    private val inImage = ComponentPin(this, null, "Image", ImageFloatArray::class.java, null)
    private val inDepth = ComponentPin(this, null, "Depth", ImageFloatArray::class.java, null)

    private val outImage = ComponentPin(this, {
        val image = inImage.receiveValue()
        val depth = inDepth.receiveValue()


        if(image != null && depth != null){
            val w = image.width
            val h = image.height

            val convertProcess = ProcessBuilder(
                    listOfNotNull(
                            "convert", // TODO: imagemagick -> opencl
                            "-define", "quantum:format=floating-point",
                            "-size", "${w}x${h}",
                            "-depth", "32",
                            "-endian", "MSB",
                            "RGBA:-",
                            "RGBA:-",
                            "-compose", "blur", "-define", "compose:args=${8 / Main.arrangementWindow.visualEditor.downscale}", "-composite",
                            "RGBA:-"
                    )
            ).start()

            kotlin.run{
                val bytes = ByteBuffer.allocate(image.array.size * 4)
                image.array.forEach { bytes.putFloat(it) }
                convertProcess.outputStream.write(bytes.array())
            }
            kotlin.run{
                val bytes = ByteBuffer.allocate(depth.array.size * 4)
                depth.array.forEach { bytes.putFloat(it) }
                convertProcess.outputStream.write(bytes.array())
            }

            convertProcess.outputStream.close()
            val convertedImageBytes = convertProcess.inputStream.readBytes()

            val convertedImage = FloatArray(convertedImageBytes.size/4)
            ByteBuffer.wrap(convertedImageBytes).asFloatBuffer().get(convertedImage)


            convertProcess.waitFor()

            ImageFloatArray(convertedImage, w, h)
        }else{
            ImageFloatArray(floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f), 1, 1)
        }


    }, "Image", ImageFloatArray::class.java, null)

    init {
        inputPins.add(inImage)
        inputPins.add(inDepth)
        outputPins.add(outImage)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}