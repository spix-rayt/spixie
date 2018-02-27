package spixie

class GraphData {
    val points = ArrayList<Point>()

    private var lastInsertedOrUpdated:Point = Point(0, 0.0)
    fun insertOrUpdatePoint(x: Int, y:Double, clearLine:Boolean) {
        var currentPoint:Point? = null
        for (point in points) {
            if(point.x == x){
                point.y = y
                currentPoint = point
                break
            }
        }
        if(currentPoint == null){
            currentPoint = Point(x, y)
            points.add(currentPoint)
        }
        sortPoints()
        if(clearLine){
            val (startPoint, endPoint) = if(currentPoint.x > lastInsertedOrUpdated.x) {
                lastInsertedOrUpdated to currentPoint
            } else {
                currentPoint to lastInsertedOrUpdated
            }
            var delStart = false
            val toDel = ArrayList<Point>()
            for (point in points) {
                if(point == endPoint){
                    break
                }
                if(delStart){
                    toDel.add(point)
                }
                if(point == startPoint){
                    delStart = true
                }
            }
            for (point in toDel) {
                points.remove(point)
            }
        }
        lastInsertedOrUpdated = currentPoint
    }

    private fun sortPoints() {
        points.sortWith(Comparator<Point> { p1, p2 -> java.lang.Integer.compare(p1.x, p2.x) })
    }

    fun getValue(time: Double): Double {
        val x = time*100.0
        var p1: Point? = null
        var p3: Point? = null
        if(points.size == 0){
            return 1.0
        }
        else if (points.size == 1) {
            return points[0].y
        } else {
            if (x < points[0].x) {
                return points[0].y
            }
            if (x > points[points.size - 1].x) {
                return points[points.size - 1].y
            }
            for (point in points) {
                if (p1 == null) {
                    p1 = point
                } else {
                    if (p3 != null) {
                        p1 = p3
                    }
                    p3 = point
                    if (p3.x >= x) {
                        val t = (x - p1.x) / (p3.x - p1.x).toDouble()
                        return p1.y + (p3.y - p1.y) * t
                    }
                }
            }
            return p3!!.y
        }
    }

    fun copy(from:Int, to:Int): ArrayList<Point> {
        var resultPoints = ArrayList<Point>()
        val iterator = points.iterator()
        while (iterator.hasNext()){
            val point = iterator.next()
            if(point.x>=from){
                resultPoints.add(Point(point.x - from, point.y))
                break
            }
        }
        while (iterator.hasNext()){
            val point = iterator.next()
            if(point.x>=to){
                break
            }else{
                resultPoints.add(Point(point.x - from, point.y))
            }
        }
        return resultPoints
    }

    fun paste(from:Int, points: ArrayList<Point>){
        for (point in points) {
            insertOrUpdatePoint(point.x + from, point.y, false)
        }
        sortPoints()
    }

    fun del(from: Int, to:Int){
        val iterator = points.iterator()
        while (iterator.hasNext()){
            val point = iterator.next()
            if(point.x>=from){
                iterator.remove()
                break
            }
        }
        while (iterator.hasNext()){
            val point = iterator.next()
            if(point.x>=to){
                break
            }else{
                iterator.remove()
            }
        }
    }
}