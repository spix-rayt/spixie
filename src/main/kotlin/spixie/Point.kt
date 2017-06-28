package spixie

class Point(var x: Double, var y: Double) {

    var gravity_x = 0.5
    var gravity_y = 0.5

    var next: Point? = null
    var prev: Point? = null

    override fun toString(): String {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                '}'
    }
}
