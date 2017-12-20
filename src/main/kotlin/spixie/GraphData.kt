package spixie

class GraphData {
    val points = ArrayList<Point>()

    init {
        insertOrUpdatePoint(0.0, 1.0, false)
        sortPoints()
    }

    private var lastInsertedOrUpdated:Point = Point(0.0, 0.0)
    fun insertOrUpdatePoint(x: Double, y:Double, clearLine:Boolean) {
        val magnet = 0.01
        val index = Math.round(x / magnet).toInt()
        var currentPoint:Point? = null
        for (point in points) {
            if(Math.round(point.x/magnet).toInt() == index){
                point.y = y
                currentPoint = point
                break
            }
        }
        if(currentPoint == null){
            currentPoint = Point(index * magnet, y)
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
        points.sortWith(Comparator<Point> { p1, p2 -> java.lang.Double.compare(p1.x, p2.x) })
    }

    fun getValue(param: Double): Double {
        var p1: Point? = null
        var p3: Point? = null
        if (points.size == 1) {
            return points[0].y
        } else {
            if (param < points[0].x) {
                return points[0].y
            }
            if (param > points[points.size - 1].x) {
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
                    if (p3.x >= param) {
                        val t = (param - p1.x) / (p3.x - p1.x)
                        return p1.y + (p3.y - p1.y)*t
                    }
                }
            }
            return p3!!.y
        }
    }
}