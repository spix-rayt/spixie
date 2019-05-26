package spixie.arrangement

import javafx.scene.canvas.Canvas
import spixie.Core
import spixie.Main
import spixie.NumberControl
import spixie.visualEditor.GraphData
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class ArrangementGraph: Externalizable {
    val data = GraphData()

    val canvas = Canvas(1.0, 100.0)

    val rangeMinControl = NumberControl(0.0, "Min")

    val rangeMaxControl = NumberControl(1.0, "Max")

    init {
        rangeMinControl.changes.subscribe {
            Core.renderManager.requestRender()
        }
        rangeMaxControl.changes.subscribe {
            Core.renderManager.requestRender()
        }
    }

    override fun readExternal(o: ObjectInput) {
        data.values.clear()
        data.values.addAll(o.readObject() as ArrayList<GraphData.Fragment>)
        rangeMinControl.value = o.readDouble()
        rangeMaxControl.value = o.readDouble()
    }

    override fun writeExternal(o: ObjectOutput) {
        o.writeObject(data.values)
        o.writeDouble(rangeMinControl.value)
        o.writeDouble(rangeMaxControl.value)
    }

    companion object {
        const val serialVersionUID = 0L
    }
}