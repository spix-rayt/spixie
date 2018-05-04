package spixie.visual_editor

import spixie.static.mix
import spixie.static.raw

class Particle {
    var red:Float = 0.0f
    var green:Float = 0.0f
    var blue:Float = 0.0f
    var alpha:Float = 0.0f
    var size:Float = 0.0f
    var x:Float = 0.0f
    var y:Float = 0.0f

    fun copy(): Particle {
        return Particle().apply {
            this.red = this@Particle.red
            this.green = this@Particle.green
            this.blue = this@Particle.blue
            this.alpha = this@Particle.alpha
            this.size = this@Particle.size
            this.x = this@Particle.x
            this.y = this@Particle.y
        }
    }

    fun spixieHash(): Long {
        return red.raw() mix  green.raw() mix blue.raw() mix alpha.raw() mix size.raw() mix x.raw() mix y.raw()
    }
}
