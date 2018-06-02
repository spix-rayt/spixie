package spixie

import javafx.scene.canvas.Canvas
import spixie.visualEditor.GraphData
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class ArrangementGraph: Externalizable {
    val data = GraphData()
    val canvas = Canvas(1.0, 100.0)
    val rangeFromControl = ValueControl(0.0, 0.1, "From")
    val rangeToControl = ValueControl(1.0, 0.1, "To")

    init {
        rangeFromControl.changes.subscribe {
            Main.renderManager.requestRender()
        }
        rangeToControl.changes.subscribe {
            Main.renderManager.requestRender()
        }
    }

    override fun readExternal(o: ObjectInput) {
        data.points = o.readObject() as FloatArray
        data.jumpPoints.putAll(o.readObject() as HashMap<Int, Pair<Float, Float>>)
        rangeFromControl.value = o.readDouble()
        rangeToControl.value = o.readDouble()
    }

    override fun writeExternal(o: ObjectOutput) {
        o.writeObject(data.points)
        o.writeObject(data.jumpPoints)
        o.writeDouble(rangeFromControl.value)
        o.writeDouble(rangeToControl.value)
    }

    companion object {
        const val serialVersionUID = 0L
    }
}