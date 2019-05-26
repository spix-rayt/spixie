package spixie.visualEditor.pins

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import spixie.Core
import spixie.visualEditor.Component
import spixie.visualEditor.ImageFloatBuffer
import java.io.ObjectInput
import java.io.ObjectOutput

class ComponentPinImageFloatBuffer(name: String): ComponentPin(name) {
    var getValue: (() -> ImageFloatBuffer)? = null

    fun receiveValue(): ImageFloatBuffer {
        return connections
                .sortedBy { it.component.layoutY }
                .mapNotNull { (it as? ComponentPinImageFloatBuffer)?.getValue?.invoke() }
                .lastOrNull() ?: ImageFloatBuffer(Core.opencl.createZeroBuffer(4), 1, 1)
    }

    override fun serialize(): JsonObject {
        val obj = JsonObject()
        obj.add("class", JsonPrimitive(this::class.qualifiedName))
        obj.add("name", JsonPrimitive(name))
        return obj
    }
}