package spixie.visualEditor.components

import spixie.Main
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ImageFloatBuffer

class Result : Component() {
    private val imageInput = ComponentPin(this, null, "Image", ImageFloatBuffer::class.java, null)
    init {
        inputPins.add(imageInput)
        updateVisual()
    }

    fun getImage(): ImageFloatBuffer{
        return imageInput.receiveValue() ?: ImageFloatBuffer(Main.opencl.createZeroBuffer(4), 1, 1)
    }

    companion object {
        const val serialVersionUID = 0L
    }
}