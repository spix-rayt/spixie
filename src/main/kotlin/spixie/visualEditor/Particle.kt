package spixie.visualEditor

import org.joml.Matrix4f
import spixie.static.mix
import spixie.static.raw
import java.nio.FloatBuffer

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
            this.matrix.set(this@Particle.matrix)
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

    fun saveTo(floatBuffer: FloatBuffer) {
        floatBuffer.put(red)
        floatBuffer.put(green)
        floatBuffer.put(blue)
        floatBuffer.put(alpha)
        floatBuffer.put(size)
        floatBuffer.put(matrix.m00())
        floatBuffer.put(matrix.m01())
        floatBuffer.put(matrix.m02())
        floatBuffer.put(matrix.m03())
        floatBuffer.put(matrix.m10())
        floatBuffer.put(matrix.m11())
        floatBuffer.put(matrix.m12())
        floatBuffer.put(matrix.m13())
        floatBuffer.put(matrix.m20())
        floatBuffer.put(matrix.m21())
        floatBuffer.put(matrix.m22())
        floatBuffer.put(matrix.m23())
        floatBuffer.put(matrix.m30())
        floatBuffer.put(matrix.m31())
        floatBuffer.put(matrix.m32())
        floatBuffer.put(matrix.m33())
    }

    fun loadFrom(floatBuffer: FloatBuffer){
        red = floatBuffer.get()
        green = floatBuffer.get()
        blue = floatBuffer.get()
        alpha = floatBuffer.get()
        size = floatBuffer.get()
        matrix.m00(floatBuffer.get())
        matrix.m01(floatBuffer.get())
        matrix.m02(floatBuffer.get())
        matrix.m03(floatBuffer.get())
        matrix.m10(floatBuffer.get())
        matrix.m11(floatBuffer.get())
        matrix.m12(floatBuffer.get())
        matrix.m13(floatBuffer.get())
        matrix.m20(floatBuffer.get())
        matrix.m21(floatBuffer.get())
        matrix.m22(floatBuffer.get())
        matrix.m23(floatBuffer.get())
        matrix.m30(floatBuffer.get())
        matrix.m31(floatBuffer.get())
        matrix.m32(floatBuffer.get())
        matrix.m33(floatBuffer.get())
    }

    companion object {
        const val PARTICLE_FLOATS = 21
    }
}
