package research.test2

import org.joml.Vector3d
import spixie.static.calcLuminance
import spixie.static.convertHueChromaLuminanceToRGB
import spixie.static.convertRGBToHueChroma
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.*
import kotlin.random.Random
import kotlin.system.measureNanoTime

const val scaleDown = 12
const val WIDTH = 7680 / scaleDown
const val HEIGHT = 4320 / scaleDown
const val NORMAL_CALC_OFFSET = 0.000001
const val RENDER_DIST = 1000.0
const val RAY_MARCHING_SPP = 20
const val RAY_MARCHING_DELTA = 0.1

const val DEBUG = false
const val CALC_LIGHT_VISIBILITY = true
const val CALC_LIGHT_VISIBILITY_STEP = 0.1

abstract class SDFObject(val name: String) {
    var light: Light? = null
    var absorptionCoefficient = 1.0

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

class Sphere(val pos: Vector3d, val radius: Double, val color: Vector3d, name: String) : SDFObject(name) {
    override fun sdf(p: Vector3d): Double {
        return p.distance(pos) - radius
    }

    override fun albedoColor(): Vector3d {
        return color
    }
}

class FoldedSpace(val offset: Vector3d, val size: Vector3d, val sdfObject: SDFObject): SDFObject("") {
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

val sdfObjects = arrayListOf<SDFObject>()

//val lights = run {
//    val result = arrayListOf<Light>()
////    result.add(Light(Vector3d(-40.0, 0.0, 20.0), Vector3d(150.0, 0.0, 0.0)))
//    result.add(Light(Vector3d(10.0, 0.0, 20.0), Vector3d(0.0, 150.0, 0.0)))
////    result.add(Light(Vector3d(0.0, 30.0, 0.0), Vector3d(0.0, 0.0, 150.0)))
////    result.add(Light(Vector3d(-20.0, 2.0, -5.0), Vector3d(13.0, 13.0, 0.0)))
//    result
//}

val lights = arrayListOf<Light>()

fun calcRandomColor(): Vector3d {
    val randomColor = javafx.scene.paint.Color.hsb(Random.nextDouble() * 360, 1.0, 0.2)
    return Vector3d(randomColor.red, randomColor.green, randomColor.blue)
}

val focalLength = 140.0
val aperture = 0.0

fun randomCircleVector(): Vector3d {
    val angle = Random.nextDouble() * Math.PI * 2
    val randX = cos(angle)
    val randY = sin(angle)
    return Vector3d(randX, randY, 0.0)
}

fun calcColor(pixelX: Int, pixelY: Int): Color {
    val spp = if(DEBUG) {
        1
    } else {
        RAY_MARCHING_SPP
    }

    var r = 0.0
    var g = 0.0
    var b = 0.0

    val cameraPos = Vector3d(0.0, 0.0, -100.0)
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
        val shiftedCameraPos = Vector3d(cameraPos).add(randomCircleVector().mul(aperture))
        val newRayDirection = Vector3d(focalPoint).sub(shiftedCameraPos).normalize()
        val sampleColor = sampleRay(shiftedCameraPos, newRayDirection, 0.0)
        r += sampleColor.x
        g += sampleColor.y
        b += sampleColor.z
    }

    val resultR = (r / spp.toDouble()).pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f) * 255.0f
    val resultG = (g / spp.toDouble()).pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f) * 255.0f
    val resultB = (b / spp.toDouble()).pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f) * 255.0f

    return Color(
        resultR.randomRoundToInt().coerceIn(0, 255),
        resultG.randomRoundToInt().coerceIn(0, 255),
        resultB.randomRoundToInt().coerceIn(0, 255)
    )
}

fun Float.randomRoundToInt(): Int {
    val maxOffset = 1.0f
    val halfMaxOffset = maxOffset / 2.0f
    val randomOffset = Random.nextFloat() * maxOffset - halfMaxOffset
    return (this + randomOffset).roundToInt()
}

fun sampleRay(rayOrigin: Vector3d, rayDirection: Vector3d, startTotalDist: Double): Vector3d {
    dbg { "===Sample Ray===" }
    var totalDist = startTotalDist
    var p = Vector3d(rayOrigin).add(Vector3d(rayDirection).mul(totalDist))
    val sdfObjectsClone = sdfObjects.toList()
    var lightAbsorbed = 1.0
    val resultColor = Vector3d(0.0)

    var i = 0
    while (true) {
        if(i >= 100000) {
            println(">$i")
            break
        }
        dbg { "POINT (${p.x} ${p.y} ${p.z})" }
        var dist = Double.MAX_VALUE
        sdfObjectsClone.forEach { obj ->
            val d = obj.sdf(p)
            if(d < RAY_MARCHING_DELTA) {
                val previousLightAbsorbed = lightAbsorbed
                dbg { "ABSORB ${obj.name}" }
                lightAbsorbed *= exp(-obj.absorptionCoefficient * RAY_MARCHING_DELTA)
                val absorptionFromMarch = previousLightAbsorbed - lightAbsorbed

                lights.forEach { light ->
                    val lightVisibility = calcLightVisibility(p, light)
                    resultColor.add(Vector3d(light.emmitance).mul(lightVisibility).mul(absorptionFromMarch).mul(obj.albedoColor()))
                }
            }
            if(d < dist) {
                dist = d
            }
        }

        if(dist < RAY_MARCHING_DELTA) {
            dist = RAY_MARCHING_DELTA
        }

        dbg { "Light Absorbed $lightAbsorbed" }
        if(lightAbsorbed < 0.0001) {
            break
        }

        totalDist += dist
        if(totalDist > RENDER_DIST) {
            break
        }
        p = Vector3d(rayOrigin).add(Vector3d(rayDirection).mul(totalDist))

        i += 1
    }

//    val fogCoefficient = 1.0 - linearstep(RENDER_DIST * 0.5, RENDER_DIST, totalDist)
//    resultColor.mul(fogCoefficient)

    return resultColor
}

fun calcLightVisibility(rayOrigin: Vector3d, light: Light): Double {
    if(CALC_LIGHT_VISIBILITY) {
        val rayDirection = Vector3d(light.pos).sub(rayOrigin).normalize()
        val sdfObjectsClone = sdfObjects.toList()
        var lightVisibility = 1.0
        var totalDist = 0.0
        var p = Vector3d(rayOrigin)
        val maxDist = light.pos.distance(rayOrigin)
        while (true) {
            var dist = Double.MAX_VALUE
            sdfObjectsClone.forEach { obj ->
                val d = obj.sdf(p)
                if(d < CALC_LIGHT_VISIBILITY_STEP) {
                    lightVisibility *= exp(-obj.absorptionCoefficient * CALC_LIGHT_VISIBILITY_STEP)
                }
                if(d < dist) {
                    dist = d
                }
            }
            if(dist < CALC_LIGHT_VISIBILITY_STEP) {
                dist = CALC_LIGHT_VISIBILITY_STEP
            }
            if(lightVisibility < 0.0001) {
                return 0.0
            }
            totalDist += dist
            if(totalDist > maxDist) {
                break
            }
            p = Vector3d(rayOrigin).add(Vector3d(rayDirection).mul(totalDist))
        }
        if(totalDist > 0.0) {
            return lightVisibility / totalDist / totalDist
        } else {
            return lightVisibility
        }
    } else {
        return 1.0
    }
}

fun dbg(block: () -> String) {
    if(DEBUG) {
        println(block())
    }
}

fun renderImage(fileName: String) {
    val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)

    val renderXs = if(DEBUG) {
        152..152
    } else {
        0 until WIDTH
    }

    val renderYs = if(DEBUG) {
        132..132
    } else {
        0 until HEIGHT
    }

    val renderTime = measureNanoTime {
        val linkedQueue = ConcurrentLinkedQueue(renderXs.toList())
        val threads = (0 until 4).map {
            thread {
                while (true) {
                    val x = linkedQueue.poll() ?: break
                    println("${(x.toDouble() / (WIDTH - 1).toDouble() * 1000.0).roundToInt() / 10.0}%")
                    for(y in renderYs) {
                        image.setRGB(x, y, calcColor(x, y).rgb)
                    }
                }
            }
        }
        threads.forEach {
            it.join()
        }
    }

    println("Render time: ${renderTime / 1000000} ms")

    ImageIO.write(image, "png", File("$fileName.png"))
}

fun changeImage() {
    val inputImage = ImageIO.read(File("pony.jpeg"))
    val outputImage = BufferedImage(inputImage.width, inputImage.height, BufferedImage.TYPE_INT_ARGB)
    for (x in 0 until inputImage.width) {
        for(y in 0 until inputImage.height) {
            val color = Color(inputImage.getRGB(x, y))
            val r = (color.red / 255.0).pow(2.2)
            val g = (color.green / 255.0).pow(2.2)
            val b = (color.blue / 255.0).pow(2.2)


            val (h, c) = convertRGBToHueChroma(r, g, b)
            val l = calcLuminance(r, g, b).coerceIn(0.0, 1.0)
            //val l = ((x + 1) / inputImage.width.toDouble()).pow(2.2)
            val (rr,gg,bb) = convertHueChromaLuminanceToRGB(h, 1.0, l, true)
            val outputColor = Color(
                rr.pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f),
                gg.pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f),
                bb.pow(1.0 / 2.2).toFloat().coerceIn(0.0f, 1.0f)
            )
            outputImage.setRGB(x, y, outputColor.rgb)
        }

        println("${x + 1}/${inputImage.width}")
    }

    ImageIO.write(outputImage, "png", File("eqlight.png"))
}

fun main() {
    //println(calcLightVisibility(Vector3d(100.0, 0.0, 60.0), lights[0]))

//    sdfObjects.add(Sphere(Vector3d(0.0, 0.0, 60.0), 13.0, Vector3d(0.0, 1.0, 0.0), "green").also {
//        it.absorptionCoefficient = 0.01
//    })
//    sdfObjects.add(Sphere(Vector3d(-26.0, 0.0, 60.0), 6.0, Vector3d(0.0, 0.0, 1.0), "blue").also {
//        it.absorptionCoefficient = 1.0
//    })

//    sdfObjects.add(Sphere(Vector3d(0.0, 0.0, 0.0), 10.0, Vector3d(1.0, 1.0, 1.0), "").also { it.absorptionCoefficient = 0.5 })
//    sdfObjects.add(Sphere(Vector3d(8.0, 0.0, 0.0), 5.0, Vector3d(1.0, 1.0, 1.0), "").also { it.absorptionCoefficient = 0.5 })
//    sdfObjects.add(Sphere(Vector3d(-20.0, 2.0, -5.0), 14.0, Vector3d(1.0, 1.0, 1.0), "").also { it.absorptionCoefficient = 0.7 })

    sdfObjects.add(Sphere(Vector3d(0.0, 0.0, 0.0), 10.0, Vector3d(1.0, 1.0, 1.0), "").also { it.absorptionCoefficient = 3.0 })


//    var f = 0
//    for(x in -40..40) {
//        lights.clear()
//        lights.add(Light(Vector3d(x.toDouble(), 0.0, 20.0), Vector3d(0.0, 150.0, 0.0)))
//        renderImage("0_f${f.toString().padStart(3, '0')}")
//        f++
//    }

    lights.clear()
    lights.add(Light(Vector3d(30.0, 0.0, -30.0), Vector3d(0.0, 150.0, 0.0)))
    renderImage("test")

//    changeImage()
}