package spixie.raymarching

import spixie.Core
import spixie.static.roundUp
import spixie.visualEditor.ImageFloatBuffer

object SplatRenderer {
    val FLOATS_IN_SPLAT = 7

    fun render(splats: List<Splat>, width: Int, height: Int): ImageFloatBuffer {
        val sortedSplats = splats.sortedBy { it.x * it.x + it.y * it.y }

        val floatArray = FloatArray(sortedSplats.size * FLOATS_IN_SPLAT)
        sortedSplats.forEachIndexed { index, splat ->
            floatArray[index * FLOATS_IN_SPLAT + 0] = splat.x.toFloat()
            floatArray[index * FLOATS_IN_SPLAT + 1] = splat.y.toFloat()
            floatArray[index * FLOATS_IN_SPLAT + 2] = splat.z.toFloat()
            floatArray[index * FLOATS_IN_SPLAT + 3] = splat.size.toFloat()
            floatArray[index * FLOATS_IN_SPLAT + 4] = splat.r.toFloat()
            floatArray[index * FLOATS_IN_SPLAT + 5] = splat.g.toFloat()
            floatArray[index * FLOATS_IN_SPLAT + 6] = splat.b.toFloat()
        }

//        val hfov = 90.0
//        val hfov = 103.0
//        val vfov = hfov.toVFov(width, height)

        val buffer = Core.opencl.renderSplats(
            floatArray,
            sortedSplats.size,
            width,
            height,
            0,
            0,
            1920.0f / 1080.0f,
            1.0f,
            1.0f
        )
        return ImageFloatBuffer(buffer, width, height, particlesCount = splats.size)
    }
}