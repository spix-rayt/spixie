package spixie.test2

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
const val RAY_MARCHING_DELTA = 0.0001

abstract class SDFObject {
    abstract fun sdf(p: Vector3d): Double

    abstract fun albedoColor(): Vector3d

    fun normal(p: Vector3d): Vector3d {
        return Vector3d(
            sdf(Vector3d(p.x + NORMAL_CALC_OFFSET, p.y, p.z)) - sdf(Vector3d(p.x - NORMAL_CALC_OFFSET, p.y, p.z)),
            sdf(Vector3d(p.x, p.y + NORMAL_CALC_OFFSET, p.z)) - sdf(Vector3d(p.x, p.y - NORMAL_CALC_OFFSET, p.z)),
            sdf(Vector3d(p.x, p.y, p.z + NORMAL_CALC_OFFSET)) - sdf(Vector3d(p.x, p.y, p.z - NORMAL_CALC_OFFSET))
        ).normalize()
    }
}

class Sphere(val pos: Vector3d, val radius: Double, val color: Vector3d) : SDFObject() {
    override fun sdf(p: Vector3d): Double {
        return p.distance(pos) - radius
    }

    override fun albedoColor(): Vector3d {
        return color
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
}

class Light(val pos: Vector3d, val emmitance: Vector3d)

fun Double.customRemainder(x: Double): Double {
    return ((this % x) + x) % x
}

fun map(start: Double, end: Double, value: Double): Double {
    return start + (end - start) * value
}

fun linearstep(start: Double, end: Double, value: Double): Double {
    return ((value - start) / (end - start)).coerceIn(0.0, 1.0)
}

val sdfObjects = run {
    val pos = Vector3d(0.0, 10.0, -60.0)
    val add = Vector3d(0.7, -0.7, 17.0)
    val result = arrayListOf<SDFObject>()
    for(i in 0..100) {
        result.add(Sphere(Vector3d(pos), 4.0, Vector3d(1.0, 1.0, 1.0)))
        pos.add(add)
    }
    result
}

val lights = arrayListOf<Light>(
//    Light(Vector3d(-7.0       ,  11.0 , -18.0), Vector3d(0.0, 1.0, 0.0)),
//    Light(Vector3d(-7.0 - 11.0,  0.0  , -18.0), Vector3d(1.0, 0.3, 1.0)),
//    Light(Vector3d(-7.0       ,  -11.0, -18.0), Vector3d(0.0, 0.0, 1.0)),
//    Light(Vector3d(-7.0 + 11.0,  0.0  , -18.0), Vector3d(1.0, 0.0, 0.0)),
    //Light(Vector3d(-30.0, 0.0, -20.0), Vector3d(0.4, 0.4, 0.0)),
    //Light(Vector3d(15.0, 10.0, -20.0), Vector3d(0.0, 0.0, 0.4))
)

val focalLength = 140.0
val aperture = 2.0

fun randomSphereVector(): Vector3d {
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
    return resultVector
}

fun calcColor(pixelX: Int, pixelY: Int): Color {
    val spp = 200

    var r = 0.0
    var g = 0.0
    var b = 0.0

    val cameraPos = Vector3d(20.0, 0.0, -100.0)
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

        val focalPoint = Vector3d(cameraPos).add(Vector3d(cameraRayDirection).mul(focalLength))
        val shiftedCameraPos = Vector3d(cameraPos).add(randomSphereVector().mul(aperture))
        val newRayDirection = Vector3d(focalPoint).sub(shiftedCameraPos).normalize()

        val sampleColor = sampleRay(shiftedCameraPos, newRayDirection, 0.0, null)
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

fun sampleRay(rayOrigin: Vector3d, rayDirection: Vector3d, startTotalDist: Double, light: Light?): Vector3d {
    val distToLight = light?.pos?.distance(rayOrigin)
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

        if(light == null) {
            if(dist <= RAY_MARCHING_DELTA && closestObject != null) {
                val normal = closestObject.normal(p)
                p.add(Vector3d(normal).mul(RAY_MARCHING_DELTA * 1.1 - dist))
                val resultColor = Vector3d(0.0)
                lights.forEach { l ->
                    val reflectRay = Vector3d(l.pos).sub(p).normalize()
                    //val sampleResult = sampleRay(p, reflectRay, 0.0, l)
                    val sampleResult = Vector3d(l.emmitance)

                    val albedoColor = closestObject.albedoColor()
                    val cosT = reflectRay.dot(normal).coerceIn(0.0, 1.0)
                    resultColor.add(sampleResult.mul(albedoColor).mul(cosT))
                }

                val fogCoefficient = 1.0 - linearstep(RENDER_DIST * 0.5, RENDER_DIST, totalDist)
                resultColor.mul(fogCoefficient)

                return resultColor
            } else {
                totalDist += dist
                if(totalDist > RENDER_DIST) {
                    return Vector3d(0.0)
                }
                p = Vector3d(rayOrigin).add(Vector3d(rayDirection).mul(totalDist))

                i += 1
            }
        } else {
            if(dist <= RAY_MARCHING_DELTA && closestObject != null) {
                return Vector3d(0.0)
            } else {
                totalDist += dist
                if(totalDist > distToLight!!) {
                    return Vector3d(light.emmitance)
                }
                p = Vector3d(rayOrigin).add(Vector3d(rayDirection).mul(totalDist))

                i += 1
            }
        }
    }
    return Vector3d(0.0, 0.0, 0.0)
}

fun main() {
    val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)

    for(i in 0..6) {
        val randomColor = javafx.scene.paint.Color.hsb(Random.nextDouble() * 360, 1.0, 0.2)
        lights.add(Light(Vector3d(Random.nextDouble() * 40.0 - 20.0, Random.nextDouble() * 40.0 - 20.0, Random.nextDouble() * -40.0), Vector3d(randomColor.red, randomColor.green, randomColor.blue)))
    }

    for(x in 0 until WIDTH) {
        for(y in 0 until HEIGHT) {
            image.setRGB(x, y, calcColor(x, y).rgb)
        }
        println("${(x.toDouble() / (WIDTH - 1).toDouble() * 1000.0).roundToInt() / 10.0}%")
    }

//    for(x in 500 until 510) {
//        for(y in 300 until 310) {
//            println("$x $y")
//            image.setRGB(x, y, calcColor(x, y).rgb)
//        }
//    }

    ImageIO.write(image, "png", File("test.png"))
}