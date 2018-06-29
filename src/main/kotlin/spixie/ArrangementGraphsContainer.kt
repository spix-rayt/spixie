package spixie

import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.roundToInt

class ArrangementGraphsContainer: Externalizable {
    val name = ValueTextControl("", "")
    val list = arrayListOf(ArrangementGraph())
    var expanded = true

    fun getValue(time: Double): Double{
        return list.fold(0.0){ acc, arrangementGraph ->
            acc + arrangementGraph.data.getValue(time) * (arrangementGraph.rangeMaxControl.value - arrangementGraph.rangeMinControl.value) + arrangementGraph.rangeMinControl.value
        }
    }

    fun getSum(time: Double): Double{
        return list.fold(0.0){ acc, arrangementGraph ->
            acc + (0..(time*100).roundToInt()).fold(0.0) {acc2, t ->
                acc2 + arrangementGraph.data.getLeftValue(t) * (arrangementGraph.rangeMaxControl.value - arrangementGraph.rangeMinControl.value) + arrangementGraph.rangeMinControl.value
            }
        }/100.0
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