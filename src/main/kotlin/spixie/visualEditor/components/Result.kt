package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ImageFloatArray

class Result : Component() {
    private val imageInput = ComponentPin(this, null, "Image", ImageFloatArray::class.java, null)
    init {
        inputPins.add(imageInput)
        updateVisual()
    }

    fun getImage(): ImageFloatArray{
        return imageInput.receiveValue() ?: ImageFloatArray(floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f), 1, 1)
    }

    companion object {
        const val serialVersionUID = 0L
    }
}