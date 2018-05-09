package spixie.visual_editor

import spixie.static.linearInterpolate

class GraphData {
    var points = floatArrayOf()

    fun resizeIfNeed(newSize: Int){
        if(newSize > points.size){
            points = points.copyOf(newSize)
        }
    }

    fun getValue(time: Double): Double {
        val x = time*100.0
        return when{
            x<0 -> 0.0
            x>=points.size - 1 -> 0.0
            else -> {
                val t = (x%1)
                val xx = x.toInt()
                return linearInterpolate(points[xx].toDouble(), points[xx+1].toDouble(), t)
            }
        }
    }

    fun getValue(index: Int): Float {
        return if(index < points.size){
            points[index]
        }else{
            0.0f
        }
    }

    fun copy(from:Int, to:Int): FloatArray {
        return points.sliceArray(from..to)
    }

    fun paste(from:Int, points: FloatArray){
        resizeIfNeed(from+points.size)
        points.forEachIndexed { index, f ->
            this.points[index+from] = f
        }
    }

    fun del(from: Int, to:Int){
        for(i in from..to){
            if(i<points.size){
                points[i] = 0.0f
            }
        }
    }

    fun reverse(from: Int, to:Int){
        for(i in from..to){
            if(i<points.size){
                points[i] = 1.0f - points[i]
            }
        }
    }
}