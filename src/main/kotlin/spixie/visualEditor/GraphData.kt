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
        val xi = x.toInt()
        return when{
            x<0 -> getRightValue(0).toDouble()
            x>=points.lastIndex -> getLeftValue(points.lastIndex).toDouble()
            else -> {
                val t = (x%1)
                linearInterpolate(getRightValue(xi).toDouble(), getLeftValue(xi+1).toDouble(), t)
            }
        }
    }

    fun getLeftValue(index: Int): Float {
        val coercedIndex = index.coerceIn(0, points.lastIndex)
        return points[coercedIndex].let {
            if(it == JUMP_POINT){
                jumpPoints[coercedIndex]!!.first
            }else{
                it
            }
        }
    }

    fun getRightValue(index: Int): Float {
        val coercedIndex = index.coerceIn(0, points.lastIndex)
        return points[coercedIndex].let {
            if(it == JUMP_POINT){
                jumpPoints[coercedIndex]!!.second
            }else{
                it
            }
        }
    }

    fun setJumpPoint(x: Int, jump: Pair<Float, Float>){
        jumpPoints[x] = jump
        points[x] = JUMP_POINT
    }

    fun copy(from:Int, to:Int): GraphData {
        val slicedPoints = points.sliceArray(from..to)
        val slicedJumps = jumpPoints.filterKeys { (from..to).contains(it) }.mapKeys { it.key - from }
        return GraphData().apply {
            points = slicedPoints
            jumpPoints.putAll(slicedJumps)
        }
    }

    fun paste(from:Int, data: GraphData){
        resizeIfNeed(from+data.points.size)
        data.points.forEachIndexed { index, f ->
            when (index) {
                0 -> {
                    val v = data.points[0]
                    if(v == JUMP_POINT){
                        setJumpPoint(from, getLeftValue(from) to data.jumpPoints[0]!!.second)
                    }else{
                        setJumpPoint(from, getLeftValue(from) to v)
                    }
                }
                data.points.lastIndex -> {
                    val v = data.points[index]
                    if(v == JUMP_POINT){
                        setJumpPoint(index+from, data.jumpPoints[index]!!.first to getRightValue(index+from))
                    }else{
                        setJumpPoint(index+from, v to getRightValue(index+from))
                    }
                }
                else -> {
                    this.points[index+from] = f
                    if(f == JUMP_POINT){
                        setJumpPoint(index+from, data.jumpPoints[index]!!)
                    }
                }
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