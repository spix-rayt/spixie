package spixie.visualEditor

import spixie.Main

class ComponentPinImageFloatBuffer(component: Component, val getValue: (() -> ImageFloatBuffer)?, name: String): ComponentPin(component, name) {
    fun receiveValue(): ImageFloatBuffer{
        val allConnections = (connections + imaginaryConnections).toSet()
        return allConnections
                .sortedBy { it.component.layoutY }
                .mapNotNull { (it as? ComponentPinImageFloatBuffer)?.getValue?.invoke() }
                .lastOrNull() ?: ImageFloatBuffer(Main.opencl.createZeroBuffer(4), 1, 1)
    }
}