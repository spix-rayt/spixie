package spixie

import java.io.Serializable

class Point(var x: Int, var y: Double): Serializable {
    override fun toString(): String {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                '}'
    }
}
