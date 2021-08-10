package spixie.visualEditor

import spixie.static.linearInterpolate

class GraphData {
    private val values = arrayListOf<Fragment>()

    fun getValue(time: Double): Double {
        val x = time * 100.0
        val xi = x.toInt()
        val t = (x % 1)
        val rightValue = getRightValue(xi)
        val leftValue = getLeftValue(xi + 1)
        return if(rightValue.isNaN() || leftValue.isNaN()) {
            Double.NaN
        } else {
            linearInterpolate(rightValue.toDouble(), leftValue.toDouble(), t)
        }
    }

    fun getLeftValue(index: Int): Float {
        var result = Float.NaN
        var right = Float.NaN
        values.forEach {
            val start = it.start
            val end = it.start + it.data.lastIndex
            if(index in start..end) {
                if(index==start) {
                    right = it.data[index-start]
                } else {
                    result = it.data[index-start]
                }
            }
        }
        return if (result.isNaN()) {
            right
        } else {
            result
        }
    }

    fun getRightValue(index: Int): Float {
        var result = Float.NaN
        var left = Float.NaN
        values.forEach {
            val start = it.start
            val end = it.start + it.data.lastIndex
            if(index in start..end){
                if(index==end){
                    left = it.data[index-start]
                }else{
                    result = it.data[index-start]
                }
            }
        }
        return if (result.isNaN()) {
            left
        } else {
            result
        }
    }

    fun add(fragment: Fragment){
        values.add(fragment)
    }

    fun copy(from: Int, to:Int): List<Fragment> {
        return if(from<to){
            values.mapNotNull {
                val start = it.start
                val end = it.start + it.data.lastIndex
                when{
                    from<=start && to>=end -> Fragment(it.start, it.data.clone())
                    from>=end -> null
                    to<=start -> null
                    from>start && to<end -> Fragment(from, it.data.sliceArray((from-start)..(to-start)))
                    from>start-> Fragment(from, it.data.sliceArray((from-start)..(end-start)))
                    to<end -> Fragment(start, it.data.sliceArray(0..(to-start)))
                    else -> null
                }
            }
        } else {
            listOf()
        }
    }

    fun delete(deleteStart: Int, deleteEnd:Int){
        if(deleteStart<deleteEnd) {
            val newValues = values.flatMap {
                val start = it.start
                val end = it.start + it.data.lastIndex
                when {
                    deleteStart<=start && deleteEnd>=end -> listOf()
                    deleteEnd<=start -> listOf(it)
                    deleteStart>=end -> listOf(it)
                    deleteStart<=start && deleteEnd<end -> listOf(Fragment(deleteEnd, it.data.sliceArray((deleteEnd-start)..(end-start))))
                    deleteStart>start && deleteEnd<end -> listOf(Fragment(start, it.data.sliceArray(0..(deleteStart-start))), Fragment(deleteEnd, it.data.sliceArray((deleteEnd-start)..(end-start))))
                    deleteStart>start && deleteEnd>=end -> listOf(Fragment(start, it.data.sliceArray(0..(deleteStart-start))))
                    else -> listOf(it) //unreachable
                }
            }
            values.clear()
            values.addAll(newValues)
        }
    }

    fun reverse(from: Int, to: Int){
        if(from<to){
            values.forEach {
                for(i in (from-it.start).coerceAtLeast(0)..(to-it.start).coerceAtMost(it.data.lastIndex)){
                    it.data[i] = 1.0f - it.data[i]
                }
            }
        }
    }

    class Fragment(val start: Int, var data: FloatArray)
}