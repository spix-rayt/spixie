package spixie.visual_editor.components

import spixie.Main
import spixie.ValueControl
import spixie.visual_editor.Component
import spixie.visual_editor.ComponentPin
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class FloatingConstant: Component(), Externalizable {
    val valueControl = ValueControl(0.0, 0.01, "")
    init {
        outputPins.add(ComponentPin(this, {
            valueControl.value.value
        }, "Value", Double::class.java))
        updateVisual()
        content.children.addAll(valueControl)

        valueControl.value.changes.subscribe {
            Main.renderManager.forceRender.onNext(Unit)
        }
    }

    override fun writeExternal(o: ObjectOutput) {
        super.writeExternal(o)
        o.writeDouble(valueControl.value.value)
    }

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
        valueControl.value.value = o.readDouble()
    }
}