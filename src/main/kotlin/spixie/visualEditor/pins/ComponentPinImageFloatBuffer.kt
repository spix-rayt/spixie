package spixie.visualEditor.pins

import spixie.Core
import spixie.visualEditor.ImageFloatBuffer

class ComponentPinImageFloatBuffer(name: String): ComponentPin(name) {
    var getValue: (() -> ImageFloatBuffer)? = null

    fun receiveValue(): ImageFloatBuffer {
        return connections
                .sortedBy { it.component.layoutY }
                .mapNotNull { (it as? ComponentPinImageFloatBuffer)?.getValue?.invoke() }
                .lastOrNull() ?: ImageFloatBuffer(Core.opencl.createZeroBuffer(4), 1, 1)
    }
}