package spixie.visualEditor

import spixie.Main

class ComponentPinImageFloatBuffer(component: Component, getValue: (() -> ImageFloatBuffer)?, name: String): ComponentPin(component, getValue, name) {
    override fun receiveValue(): ImageFloatBuffer{
        val allConnections = (connections + imaginaryConnections).toSet()
        return allConnections
                .sortedBy { it.component.layoutY }
                .mapNotNull { it.getValue?.invoke() as? ImageFloatBuffer }
                .lastOrNull() ?: ImageFloatBuffer(Main.opencl.createZeroBuffer(4), 1, 1)
    }
}