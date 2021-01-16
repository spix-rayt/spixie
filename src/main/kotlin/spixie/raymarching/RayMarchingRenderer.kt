package spixie.raymarching

import spixie.Core
import spixie.raymarching.geometryobject.GeometryObject
import spixie.static.roundUp
import spixie.visualEditor.ImageFloatBuffer

object RayMarchingRenderer {
    fun render(objects: List<GeometryObject>, downScale: Int): ImageFloatBuffer {
        val width = (1920 / downScale).roundUp(2)
        val height = (1080 / downScale).roundUp(2)

        val floatArray = FloatArray(objects.size * GeometryObject.FLOAT_ARRAY_SIZE)
        objects.forEachIndexed { objectIndex, geometryObject ->
            for(i in 0 until 16) {
                floatArray[objectIndex * GeometryObject.FLOAT_ARRAY_SIZE + i] = geometryObject.floats[i]
            }
        }

//        val hfov = 90.0
//        val hfov = 103.0
//        val vfov = hfov.toVFov(width, height)

        val buffer = Core.opencl.rayMarchingRender(
            floatArray,
            objects.size,
            width,
            height,
            0,
            0,
            1920.0f / 1080.0f,
            1.0f,
            1.0f
        )
        val image = ImageFloatBuffer(buffer, width, height, particlesCount = 0)

        return image
    }
}