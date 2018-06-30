package spixie.arrangement

import spixie.TextControl
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.roundToInt

class ArrangementGraphsContainer: Externalizable {
    val name = TextControl("", "")
    val list = arrayListOf(ArrangementGraph())
    var expanded = true

    fun getValue(time: Double): Double{
        return list.fold(0.0){ acc, arrangementGraph ->
            val v = arrangementGraph.data.getValue(time).let { if(it.isNaN()) 0.0 else it }
            acc + v * (arrangementGraph.rangeMaxControl.value - arrangementGraph.rangeMinControl.value) + arrangementGraph.rangeMinControl.value
        }
    }

    fun getSum(time: Double): Double{
        return list.fold(0.0){ acc, arrangementGraph ->
            acc + (0..(time*100).roundToInt()).fold(0.0) {acc2, t ->
                val v = arrangementGraph.data.getLeftValue(t).let { if(it.isNaN()) 0.0f else it }
                acc2 + v * (arrangementGraph.rangeMaxControl.value - arrangementGraph.rangeMinControl.value) + arrangementGraph.rangeMinControl.value
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