package spixie

class Particle {
    var red:Float = 0.0f
    var green:Float = 0.0f
    var blue:Float = 0.0f
    var alpha:Float = 0.0f
    var size:Float = 0.0f
    var vx:Float = 0.0f
    var vy:Float = 0.0f
    var x:Float = 0.0f
    var y:Float = 0.0f

    fun step(time:Double){
        x+=(vx*time).toFloat()
        y+=(vy*time).toFloat()
    }
}
