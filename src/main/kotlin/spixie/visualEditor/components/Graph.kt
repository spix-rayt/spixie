package spixie.visualEditor.components

import javafx.scene.layout.VBox
import spixie.Main
import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class Graph: Component(), Externalizable {
    private val valueControl = ValueControl(0.0, 1.0, "Graph ID").limitMin(0.0)
    private val rangeFromControl = ValueControl(0.0, 0.1, "From")
    private val rangeToControl = ValueControl(0.0, 0.1, "To")
    init {
        outputPins.add(ComponentPin(this, {
            (Main.arrangementWindow.graphs[valueControl.value.toInt()]?.data?.getValue(Main.arrangementWindow.visualEditor.time)
                    ?: 0.0) * (rangeToControl.value - rangeFromControl.value) + rangeFromControl.value
        }, "Graph", Double::class.java, null))
        updateVisual()
        content.children.addAll(VBox(valueControl, rangeFromControl, rangeToControl))

        valueControl.changes.subscribe {
            Main.renderManager.requestRender()
        }
    }

    override fun writeExternal(o: ObjectOutput) {
        super.writeExternal(o)
        o.writeDouble(valueControl.value)
        o.writeDouble(rangeFromControl.value)
        o.writeDouble(rangeToControl.value)
    }

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
        valueControl.value = o.readDouble()
        rangeFromControl.value = o.readDouble()
        rangeToControl.value = o.readDouble()
    }
}