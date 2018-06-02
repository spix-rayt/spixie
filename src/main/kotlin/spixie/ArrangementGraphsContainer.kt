package spixie

import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class ArrangementGraphsContainer: Externalizable {
    val name = ValueTextControl("", "")
    val list = arrayListOf(ArrangementGraph())
    var expanded = true

    fun getValue(time: Double): Double{
        return list.fold(0.0){ acc, arrangementGraph ->
            acc + arrangementGraph.data.getValue(time) * (arrangementGraph.rangeToControl.value - arrangementGraph.rangeFromControl.value) + arrangementGraph.rangeFromControl.value
        }
    }

    override fun readExternal(o: ObjectInput) {
        name.value = o.readUTF()
        list.clear()
        list.addAll(o.readObject() as ArrayList<ArrangementGraph>)
    }

    override fun writeExternal(o: ObjectOutput) {
        o.writeUTF(name.value)
        o.writeObject(list)
    }

    companion object {
        const val serialVersionUID = 0L
    }
}