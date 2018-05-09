package spixie.visual_editor

import org.joml.*
import spixie.static.mix
import spixie.static.raw

class Particle {
    var red:Float = 1.0f
    var green:Float = 1.0f
    var blue:Float = 1.0f
    var alpha:Float = 1.0f
    var size:Float = 1.0f
    val matrix = Matrix4f()

    fun copy(): Particle {
        return Particle().apply {
            this.red = this@Particle.red
            this.green = this@Particle.green
            this.blue = this@Particle.blue
            this.alpha = this@Particle.alpha
            this.size = this@Particle.size
        }
    }

    fun spixieHash(): Long {
        return red.raw() mix
                green.raw() mix
                blue.raw() mix
                alpha.raw() mix
                size.raw() mix
                matrix.m00().raw() mix
                matrix.m01().raw() mix
                matrix.m02().raw() mix
                matrix.m03().raw() mix
                matrix.m10().raw() mix
                matrix.m11().raw() mix
                matrix.m12().raw() mix
                matrix.m13().raw() mix
                matrix.m20().raw() mix
                matrix.m21().raw() mix
                matrix.m22().raw() mix
                matrix.m23().raw() mix
                matrix.m30().raw() mix
                matrix.m31().raw() mix
                matrix.m32().raw() mix
                matrix.m33().raw()
    }
}
