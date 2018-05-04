package spixie.visual_editor.components

import spixie.Main
import spixie.ValueControl
import spixie.visual_editor.Component
import spixie.visual_editor.ComponentPin

class FloatingConstant: Component() {
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
}