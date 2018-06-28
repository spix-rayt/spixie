package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinImageFloatBuffer
import spixie.visualEditor.ImageFloatBuffer

class ImageResult : Component() {
    private val imageInput = ComponentPinImageFloatBuffer(this, null, "Image")
    init {
        inputPins.add(imageInput)
        updateVisual()
    }

    fun getImage(): ImageFloatBuffer{
        return imageInput.receiveValue()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}