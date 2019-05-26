package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinImageFloatBuffer
import spixie.visualEditor.ImageFloatBuffer

class ImageResult : Component() {
    private val imageInput by lazyPinFromListOrCreate(0) { ComponentPinImageFloatBuffer("Image") }

    override fun creationInit() {
        inputPins.add(imageInput)
    }

    override fun configInit() {
        updateUI()
    }

    fun getImage(): ImageFloatBuffer{
        return imageInput.receiveValue()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}