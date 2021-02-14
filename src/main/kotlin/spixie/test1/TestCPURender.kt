package spixie.test1

import org.joml.Vector3d
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.random.Random

const val scaleDown = 2
const val WIDTH = 1920 / scaleDown
const val HEIGHT = 1080 / scaleDown
const val NORMAL_CALC_OFFSET = 0.000001
const val RENDER_DIST = 6000.0
const val RENDER_DEPTH = 2
const val RAY_MARCHING_DELTA = 0.0001

abstract class SDFObject {
    abstract fun sdf(p: Vector3d): Double

    abstract fun albedoColor(): Vector3d

    abstract fun emmitanceColor(): Vector3d

    fun normal(p: Vector3d): Vector3d {
        return Vector3d(
            sdf(Vector3d(p.x + NORMAL_CALC_OFFSET, p.y, p.z)) - sdf(Vector3d(p.x - NORMAL_CALC_OFFSET, p.y, p.z)),
            sdf(Vector3d(p.x, p.y + NORMAL_CALC_OFFSET, p.z)) - sdf(Vector3d(p.x, p.y - NORMAL_CALC_OFFSET, p.z)),
            sdf(Vector3d(p.x, p.y, p.z + NORMAL_CALC_OFFSET)) - sdf(Vector3d(p.x, p.y, p.z - NORMAL_CALC_OFFSET))
        ).normalize()
    }
}

class Sphere(val pos: Vector3d, val radius: Double, val color: Vector3d, val emmitance: Vector3d) : SDFObject() {
    override fun sdf(p: Vector3d): Double {
        return p.distance(pos) - radius
    }

    override fun albedoColor(): Vector3d {
        return color
    }

    override fun emmitanceColor(): Vector3d {
        return emmitance
    }
}

class FoldedSpace(val offset: Vector3d, val size: Vector3d, val sdfObject: SDFObject): SDFObject() {
    override fun sdf(p: Vector3d): Double {
        return sdfObject.sdf(
            Vector3d(
                (p.x + offset.x).customRemainder(size.x),
                (p.y + offset.y).customRemainder(size.y),
                (p.z + offset.z).customRemainder(size.z)
            )
        )
    }

    override fun albedoColor(): Vector3d {
        return sdfObject.albedoColor()
    }

    override fun emmitanceColor(): Vector3d {
        return sdfObject.emmitanceColor()
    }
}

fun Double.customRemainder(x: Double): Double {
    return ((this % x) + x) % x
}

fun map(start: Double, end: Double, value: Double): Double {
    return start + (end - start) * value
}

fun linearstep(start: Double, end: Double, value: Double): Double {
    return ((value - start) / (end - start)).coerceIn(0.0, 1.0)
}

//val sdfObjects = arrayListOf<SDFObject>(
//    FoldedSpace(Vector3d(0.0, 0.0, 0.0), Vector3d(9.0, 9.0, 9.0), Sphere(Vector3d(4.5, 4.5, 4.5), 0.8, Vector3d(0.0, 0.0, 0.0), Vector3d(1.0, 0.3, 1.0))),
//    FoldedSpace(Vector3d(4.5, 4.5, 4.5), Vector3d(9.0, 9.0, 9.0), Sphere(Vector3d(4.5, 4.5, 4.5), 0.8, Vector3d(1.0, 1.0, 1.0), Vector3d(0.0, 0.0, 0.0)))
//)

val sdfObjects = arrayListOf<SDFObject>(
    Sphere(Vector3d(-7.0, 0.0, 0.0), 12.0, Vector3d(1.0, 1.0, 1.0), Vector3d(0.0, 0.0, 0.0)),
    Sphere(Vector3d(-7.0       ,  11.0 , -11.0), 7.0 - 1.3 * 0.0, Vector3d(0.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0)),
    Sphere(Vector3d(-7.0 - 11.0,  0.0  , -11.0), 7.0 - 1.3 * 1.0, Vector3d(0.0, 0.0, 0.0), Vector3d(1.0, 0.3, 1.0)),
    Sphere(Vector3d(-7.0       ,  -11.0, -11.0), 7.0 - 1.3 * 2.0, Vector3d(0.0, 0.0, 0.0), Vector3d(0.0, 0.0, 1.0)),
    Sphere(Vector3d(-7.0 + 11.0,  0.0  , -11.0), 7.0 - 1.3 * 3.0, Vector3d(0.0, 0.0, 0.0), Vector3d(1.0, 0.0, 0.0)),
)

fun calcColor(pixelX: Int, pixelY: Int): Color {
    val spp = 30

    var r = 0.0
    var g = 0.0
    var b = 0.0

    val cameraPos = Vector3d(-7.0, 0.0, -100.0)
    val screenWidth = WIDTH.toDouble() / HEIGHT.toDouble()
    val screenHeight = 1.0
    val pixelWidth = screenWidth * 2.0 / (WIDTH - 1).toDouble()
    val pixelHeight = screenHeight * 2.0 / (HEIGHT - 1).toDouble()


    for(j in 1..spp) {
        val cameraRayDirection = Vector3d(
            -screenWidth + pixelWidth * pixelX + (Math.random() - 0.5) * pixelWidth,
            -screenHeight + pixelHeight * pixelY + (Math.random() - 0.5) * pixelHeight,
            3.0
        ).normalize()

        val sampleColor = sampleRay(cameraPos, cameraRayDirection, 1, 0.0)
        r += sampleColor.x
        g += sampleColor.y
        b += sampleColor.z
    }

    return Color(
        (r / spp.toDouble()).pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f),
        (g / spp.toDouble()).pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f),
        (b / spp.toDouble()).pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f)
    )
}

fun Vector3d.randomHemisphere(): Vector3d {
    val randX = Random.nextDouble()
    val randY = Random.nextDouble()
    val cosTheta = sqrt(1.0 - randX)
    val sinTheta = sqrt(randX)
    val phi = 2.0 * Math.PI * randY
    val resultVector = Vector3d(
        cos(phi) * sinTheta,
        sin(phi) * sinTheta,
        cosTheta
    )
    if(resultVector.dot(this) < 0.0) {
        resultVector.negate()
    }
    return resultVector
}

fun sampleRay(rayOrigin: Vector3d, rayDirection: Vector3d, depth: Int, startTotalDist: Double): Vector3d {
    if(depth > RENDER_DEPTH) {
        return Vector3d(0.0, 0.0, 0.0)
    }

    var totalDist = startTotalDist
    var p = Vector3d(rayOrigin).add(Vector3d(rayDirection).mul(totalDist))

    var i = 0
    while (i < 100000) {
        var dist = Double.MAX_VALUE
        val closestObject = run {
            var kek: SDFObject? = null
            sdfObjects.forEach { obj ->
                val d = obj.sdf(p)
                if(d < dist) {
                    dist = d
                    kek = obj
                }
            }
            kek
        }

        if(dist <= RAY_MARCHING_DELTA && closestObject != null) {
            val normal = closestObject.normal(p)
            p.add(Vector3d(normal).mul(RAY_MARCHING_DELTA - dist))
            val randomReflectRay = normal.randomHemisphere()
            val sampleResult = sampleRay(p, randomReflectRay, depth + 1, 0.0)

            val albedoColor = closestObject.albedoColor()
            val emmitanceColor = closestObject.emmitanceColor()
            val cosT = randomReflectRay.dot(normal)

            val resultColor = sampleResult.mul(albedoColor).mul(cosT).add(emmitanceColor)

            if(depth == 1) {
                val fogCoefficient = 1.0 - linearstep(RENDER_DIST * 0.5, RENDER_DIST, totalDist)
                resultColor.mul(fogCoefficient)
            }

            return resultColor
        } else {
            totalDist += dist
            if(totalDist > RENDER_DIST) {
                return Vector3d(0.0, 0.0, 0.0)
            }
            p = Vector3d(rayOrigin).add(Vector3d(rayDirection).mul(totalDist))

            i += 1
        }
    }
    return Vector3d(0.0, 0.0, 0.0)
}

fun main() {
    val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)

    for(x in 0 until WIDTH) {
        for(y in 0 until HEIGHT) {
            image.setRGB(x, y, calcColor(x, y).rgb)
        }
        println("${(x.toDouble() / (WIDTH - 1).toDouble() * 1000.0).roundToInt() / 10.0}%")
    }

    ImageIO.write(image, "png", File("test.png"))
}