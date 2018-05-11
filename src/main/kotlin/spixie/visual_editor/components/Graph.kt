package spixie.visual_editor.components

import javafx.scene.layout.VBox
import spixie.Main
import spixie.ValueControl
import spixie.visual_editor.Component
import spixie.visual_editor.ComponentPin
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class Graph: Component(), Externalizable {
    val valueControl = ValueControl(0.0, 1.0, "Graph ID").limitMin(0.0)
    val rangeFromControl = ValueControl(0.0, 0.1, "From")
    val rangeToControl = ValueControl(0.0, 0.1, "To")
    init {
        outputPins.add(ComponentPin(this, {
            (Main.arrangementWindow.graphs[valueControl.value.value.toInt()]?.data?.getValue(Main.arrangementWindow.visualEditor.time)
                    ?: 0.0) * (rangeToControl.value.value - rangeFromControl.value.value) + rangeFromControl.value.value
        }, "Graph", Double::class.java, null))
        updateVisual()
        content.children.addAll(VBox(valueControl, rangeFromControl, rangeToControl))

        valueControl.value.changes.subscribe {
            Main.renderManager.requestRender()
        }
    }

    override fun writeExternal(o: ObjectOutput) {
        super.writeExternal(o)
        o.writeDouble(valueControl.value.value)
        o.writeDouble(rangeFromControl.value.value)
        o.writeDouble(rangeToControl.value.value)
    }

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
        valueControl.value.value = o.readDouble()
        rangeFromControl.value.value = o.readDouble()
        rangeToControl.value.value = o.readDouble()
    }
}