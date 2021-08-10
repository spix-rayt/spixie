package spixie.timeline

import spixie.TextControl
import kotlin.math.roundToInt

class ArrangementGraphsContainer {
    val name = TextControl("", "")

    val list = arrayListOf(ArrangementGraph())

    var expanded = true

    fun getValue(time: Double): Double {
        return list.sumOf { arrangementGraph ->
            val v = arrangementGraph.data.getValue(time).let { if(it.isNaN()) 0.0 else it }
            v * (arrangementGraph.rangeMaxControl.value - arrangementGraph.rangeMinControl.value) + arrangementGraph.rangeMinControl.value
        }
    }

    fun getSum(time: Double): Double {
        return list.sumOf { arrangementGraph ->
            var s = 0.0
            val steps = (time * 100.0).roundToInt()
            for (step in 0..steps) {
                val t = time / steps * step
                val v = arrangementGraph.data.getValue(t).let { if(it.isNaN()) 0.0 else it }
                s += v * (arrangementGraph.rangeMaxControl.value - arrangementGraph.rangeMinControl.value) + arrangementGraph.rangeMinControl.value
            }
            s
        } / 100.0
    }
}