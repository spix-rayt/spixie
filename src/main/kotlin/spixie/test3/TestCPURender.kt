package spixie.test3

import org.joml.Vector3d
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.*
import kotlin.random.Random
import kotlin.system.measureNanoTime

const val scaleDown = 4
const val WIDTH = 4000 / scaleDown
const val HEIGHT = 4000 / scaleDown
const val RENDER_DIST = 1000.0
const val SPP = 200

const val DEBUG = false

class Point(val pos: Vector3d, val size: Double, val color: Vector3d)

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

val points = arrayListOf<Point>()
val lights = arrayListOf<Light>()

fun calcRandomColor(): Vector3d {
    val randomColor = javafx.scene.paint.Color.hsb(Random.nextDouble() * 360, 1.0, 0.2)
    return Vector3d(randomColor.red, randomColor.green, randomColor.blue)
}

val focalLength = 60.0
val aperture = 0.4

fun randomCircleVector(): Vector3d {
    val angle = Random.nextDouble() * Math.PI * 2
    val randX = cos(angle)
    val randY = sin(angle)
    return Vector3d(randX, randY, 0.0)
}

val cameraPos = Vector3d(0.0, 0.0, -30.0)

fun calcColor(pixelX: Int, pixelY: Int): Color {
    val spp = if(DEBUG) {
        1
    } else {
        SPP
    }

    var r = 0.0
    var g = 0.0
    var b = 0.0

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
        val shiftedCameraPos = Vector3d(cameraPos).add(randomCircleVector().mul(Random.nextDouble(0.0, aperture)))
        val newRayDirection = Vector3d(focalPoint).sub(shiftedCameraPos).normalize()
        val sampleColor = sampleRay(shiftedCameraPos, newRayDirection)
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

fun sampleRay(rayOrigin: Vector3d, rayDirection: Vector3d): Vector3d {
    dbg { "===Sample Ray===" }

    val resultColor = Vector3d(0.0)

    points.forEach { q ->
        val dot = Vector3d(rayDirection).dot(Vector3d(q.pos).sub(rayOrigin))
        val projectedPosition = Vector3d(rayOrigin).add(Vector3d(rayDirection).mul(dot))
        val dist = projectedPosition.distance(q.pos)
        if(dist <= q.size) {
            resultColor.set(q.color)
            return resultColor
        }
    }

//    val fogCoefficient = 1.0 - linearstep(RENDER_DIST * 0.5, RENDER_DIST, totalDist)
//    resultColor.mul(fogCoefficient)

    return resultColor
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

fun main() {
    val direction = randomCircleVector()
    direction.z = 3.0
    direction.normalize(7.0)
    val startPoint = Vector3d(0.0, 0.0, 0.0)

    for(i in 0..200) {
        points.add(Point(Vector3d(startPoint), 0.8, calcRandomColor()))
        startPoint.add(direction)
    }

    points.sortBy { it.pos.distance(cameraPos) }

    renderImage("test")
}