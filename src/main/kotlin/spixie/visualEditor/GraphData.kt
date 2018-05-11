package spixie.visualEditor

import spixie.static.linearInterpolate

class GraphData {
    var points = floatArrayOf()
    val jumpPoints = hashMapOf<Int, Pair<Float, Float>>()

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
                linearInterpolate(getRightValue(xx).toDouble(), getLeftValue(xx+1).toDouble(), t)
            }
        }
    }

    fun getLeftValue(index: Int): Float {
        return if(index < points.size){
            points[index].let {
                if(it == JUMP_POINT){
                    jumpPoints[index]!!.first
                }else{
                    it
                }
            }
        }else{
            0.0f
        }
    }

    fun getRightValue(index: Int): Float {
        return if(index < points.size){
            points[index].let {
                if(it == JUMP_POINT){
                    jumpPoints[index]!!.second
                }else{
                    it
                }
            }
        }else{
            0.0f
        }
    }

    fun setJumpPoint(x: Int, jump: Pair<Float, Float>){
        jumpPoints[x] = jump
        points[x] = JUMP_POINT
    }

    fun copy(from:Int, to:Int): Pair<FloatArray, Map<Int, Pair<Float, Float>>> {
        val slicedPoints = points.sliceArray(from..to)
        val slicedJumps = jumpPoints.filterKeys { (from..to).contains(it) }.mapKeys { it.key - from }
        return slicedPoints to slicedJumps
    }

    fun paste(from:Int, data: Pair<FloatArray, Map<Int, Pair<Float, Float>>>){
        resizeIfNeed(from+data.first.size)
        data.first.forEachIndexed { index, f ->
            when (index) {
                0 -> {
                    setJumpPoint(from, getLeftValue(from) to data.first[0])
                }
                data.first.lastIndex -> {
                    setJumpPoint(index+from, data.first[index] to getRightValue(index+from))
                }
                else -> {
                    this.points[index+from] = f
                }
            }
        }
        data.second.forEach { i, jump ->
            if(i != 0 && i != data.first.lastIndex){
                setJumpPoint(i+from, jump)
            }
        }
    }

    fun del(start: Int, end:Int){
        val startValue = getLeftValue(start)
        val endValue = getRightValue(end)
        resizeIfNeed(end+1)
        for(i in start..end){
            val t = (i - start) / (end - start).toDouble()
            points[i] = linearInterpolate(startValue.toDouble(), endValue.toDouble(), t).toFloat()
        }
    }

    fun reverse(from: Int, to:Int){
        for(i in from..to){
            if(i<points.size){
                val v = points[i]
                if(v != JUMP_POINT){
                    points[i] = 1.0f - v
                }else{
                    when(i){
                        from -> {
                            jumpPoints[i]?.let {
                                jumpPoints[i] = it.first to (1.0f - it.second)
                            }
                        }
                        to -> {
                            jumpPoints[i]?.let {
                                jumpPoints[i] = (1.0f - it.first) to it.second
                            }
                        }
                        else ->{
                            jumpPoints[i]?.let {
                                jumpPoints[i] = (1.0f - it.first) to (1.0f - it.second)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val JUMP_POINT = -1.0f
    }
}