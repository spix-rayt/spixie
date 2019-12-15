package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinImageFloatBuffer
import spixie.visualEditor.ImageFloatBuffer

class ImageResult : Component() {
    private val imageInput = ComponentPinImageFloatBuffer("Image")

    init {
        inputPins.add(imageInput)
        updateUI()
    }

    fun getImage(): ImageFloatBuffer{
        return imageInput.receiveValue()
    }
}