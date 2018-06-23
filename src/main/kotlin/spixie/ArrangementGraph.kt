package spixie

import javafx.scene.canvas.Canvas
import spixie.visualEditor.GraphData
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class ArrangementGraph: Externalizable {
    val data = GraphData()
    val canvas = Canvas(1.0, 100.0)
    val rangeMinControl = NumberControl(0.0, 0.1, "Min")
    val rangeMaxControl = NumberControl(1.0, 0.1, "Max")

    init {
        rangeMinControl.changes.subscribe {
            Main.renderManager.requestRender()
        }
        rangeMaxControl.changes.subscribe {
            Main.renderManager.requestRender()
        }
    }

    override fun readExternal(o: ObjectInput) {
        data.points = o.readObject() as FloatArray
        data.jumpPoints.putAll(o.readObject() as HashMap<Int, Pair<Float, Float>>)
        rangeMinControl.value = o.readDouble()
        rangeMaxControl.value = o.readDouble()
    }

    override fun writeExternal(o: ObjectOutput) {
        o.writeObject(data.points)
        o.writeObject(data.jumpPoints)
        o.writeDouble(rangeMinControl.value)
        o.writeDouble(rangeMaxControl.value)
    }

    companion object {
        const val serialVersionUID = 0L
    }
}