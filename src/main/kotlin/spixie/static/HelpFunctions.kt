package spixie.static

import com.jogamp.opencl.CLBuffer
import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.input.DataFormat
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Pane
import org.apache.commons.lang3.math.Fraction
import spixie.Core
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.FloatBuffer
import java.util.concurrent.CountDownLatch
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.plugins.jpeg.JPEGImageWriteParam

fun rand(p0:Long, p1:Long, p2:Long, p3:Long, p4:Long, p5:Long): Float {
    val nozerop0 = (p0 and  0x7FFFFFFFFFFFFFFF) + 1
    val nozerop1 = (p1 and  0x7FFFFFFFFFFFFFFF) + 1
    val nozerop2 = (p2 and  0x7FFFFFFFFFFFFFFF) + 1
    val nozerop3 = (p3 and  0x7FFFFFFFFFFFFFFF) + 1
    val nozerop4 = (p4 and  0x7FFFFFFFFFFFFFFF) + 1
    val nozerop5 = (p5 and  0x7FFFFFFFFFFFFFFF) + 1

    var x:Long = Long.MAX_VALUE
    x = x * nozerop0 * nozerop0 + 7046029254386353087L
    x = x xor (x shl 21)
    x = x xor (x ushr 35)
    x = x xor (x shl 4)
    x = x * nozerop1 * nozerop1 + 7046029254386353087L
    x = x xor (x shl 21)
    x = x xor (x ushr 35)
    x = x xor (x shl 4)
    x = x * nozerop2 * nozerop2 + 7046029254386353087L
    x = x xor (x shl 21)
    x = x xor (x ushr 35)
    x = x xor (x shl 4)
    x = x * nozerop3 * nozerop3 + 7046029254386353087L
    x = x xor (x shl 21)
    x = x xor (x ushr 35)
    x = x xor (x shl 4)
    x = x * nozerop4 * nozerop4 + 7046029254386353087L
    x = x xor (x shl 21)
    x = x xor (x ushr 35)
    x = x xor (x shl 4)
    x = x * nozerop5 * nozerop5 + 7046029254386353087L
    x = x xor (x shl 21)
    x = x xor (x ushr 35)
    x = x xor (x shl 4)
    x = x and 0x7FFFFFFFFFFFFFFF
    return ((x % 0xFFFFFFFFFFFFFFF).toFloat()/0xFFFFFFFFFFFFFFF)
}

fun frameToTime(frame:Int, bpm:Double): Double {
    return bpm/3600*frame
}

const val MAGIC = 0x5bd1e995
infix fun Long.mix(n:Long):Long{
    var q1 = this * MAGIC
    q1 = q1 xor (q1 ushr 48)
    q1 *= MAGIC
    val q2 = n * MAGIC
    return q1 xor q2
}

fun Double.raw():Long{
    return java.lang.Double.doubleToRawLongBits(this)
}

fun Float.raw():Long{
    return this.toDouble().raw()
}

fun BufferedImage.toJPEGByteArray(quality: Float):ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.getImageWritersByFormatName("jpg").next().run {
        output = ImageIO.createImageOutputStream(byteArrayOutputStream)
        write(
                null,
                IIOImage(this@toJPEGByteArray, null, null),
                JPEGImageWriteParam(null).apply {
                    compressionMode = JPEGImageWriteParam.MODE_EXPLICIT
                    compressionQuality = quality
                }
        )
    }
    return byteArrayOutputStream.toByteArray()
}

fun InputStream.printAvailable(){
    val available = this.available()
    if(available>0){
        val byteArray = ByteArray(available)
        this.read(byteArray)
        System.out.write(byteArray)
    }
}

fun runInUIAndWait(work: () -> Unit){
    if(Platform.isFxApplicationThread()){
        work()
    }else{
        val latch = CountDownLatch(1)
        Platform.runLater {
            work()
            latch.countDown()
        }
        latch.await()
    }
}

fun Int.roundUp(multiplicity: Int): Int{
    val r = this % multiplicity
    return if(r == 0){
        this
    }else{
        this + multiplicity - r
    }
}

fun linearInterpolate(y1: Double, y2:Double, t:Double): Double {
    return (y1*(1.0-t)+y2*t)
}

fun perlinInterpolate(y1: Double, y2: Double, t:Double):Double{
    return linearInterpolate(y1, y2, t * t * t * (t * (t * 6 - 15) + 10))
}

fun hue2rgb(p: Double,q: Double,t: Double): Double{
    val tt = when{
        t<0 -> t+1.0
        t>1 -> t-1.0
        else -> t
    }
    return when{
        tt<1.0/6.0 -> p+(q-p)*6.0*tt
        tt<1.0/2.0 -> q
        tt<2.0/3.0 -> p+(q-p)*(2.0/3.0-tt)*6.0
        else -> p
    }
}

fun convertHueChromaLuminanceToRGB(h:Double, c:Double, l:Double, clampDesaturate: Boolean): Triple<Double, Double, Double> {
    if(clampDesaturate) {
        var rangeA = 0.0
        var rangeB = 10.0
        var r = 0.0
        var g = 0.0
        var b = 0.0
        for(i in 0..20) {
            val m = (rangeA + rangeB) / 2.0
            val q = if(m < 0.5) m * (1 + c) else m + c - m * c

            val p = 2 * m - q

            r = Math.pow(hue2rgb(p,q,h+1.0/3.0), 2.2)
            g = Math.pow(hue2rgb(p,q,h), 2.2)
            b = Math.pow(hue2rgb(p,q,h-1.0/3.0), 2.2)
            if(calcLuminance(r,g,b)>l) {
                rangeB = m
            } else {
                rangeA = m
            }
        }
        return Triple(r,g,b)
    } else {
        val q = 1 + c
        val p = 2 - q
        val rn = hue2rgb(p, q, h + 1.0 / 3.0)
        val gn = hue2rgb(p, q, h)
        val bn = hue2rgb(p, q, h - 1.0 / 3.0)

        //Maxima
        //assume(l>0);
        //solve([l=0.2126*(r*k)^2.2+0.7152*(g*k)^2.2+0.0722*(b*k)^2.2], [k]);
        val k = Math.pow(5000.0, 5.0/11.0)*Math.pow(l, 5.0/11.0)/Math.pow(1063.0*Math.pow(rn,11.0/5.0) + 3576.0*Math.pow(gn,11.0/5.0) + 361.0*Math.pow(bn,11.0/5.0),5.0/11.0)

        val r = rn * k
        val g = gn * k
        val b = bn * k
        return Triple(Math.pow(r, 2.2), Math.pow(g, 2.2), Math.pow(b, 2.2))
    }
}

fun convertRGBToHueChroma(r:Double, g:Double, b:Double): Pair<Double, Double> {
    val max = maxOf(r,g,b)
    val min = minOf(r,g,b)
    var h=(max+min)/2.0
    val d=  max-min
    val s = d/(max+min)
    when(max){
        r-> h=(g-b)/d+(if(g<b) 6.0 else 0.0)
        g-> h=(b-r)/d+2.0
        b->h=(r-g)/d+4.0
    }
    return h/6 to s
}

const val Pr = 0.2126
const val Pg = 0.7152
const val Pb = 0.0722

fun calcLuminance(r:Double, g:Double, b:Double): Double{
    return r*Pr + g*Pg + b*Pb
}

fun CLBuffer<FloatBuffer>.toBufferedImage(width: Int, height: Int): BufferedImage {
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
    val forSave = Core.opencl.forSave(this, width, height)
    this.release()
    val floatArray = Core.opencl.readAndRelease(forSave)
    bufferedImage.raster.setPixels(0, 0, width, height, floatArray)
    return bufferedImage
}

fun Pane.initCustomPanning(content:Group, allDirections: Boolean){
    var mouseXOnStartDrag = 0.0
    var mouseYOnStartDrag = 0.0
    var layoutXOnStartDrag = 0.0
    var layoutYOnStartDrag = 0.0

    addEventHandler(MouseEvent.MOUSE_PRESSED) { event->
        if(event.button == MouseButton.MIDDLE){
            mouseXOnStartDrag = event.screenX
            mouseYOnStartDrag = event.screenY
            layoutXOnStartDrag = content.layoutX
            layoutYOnStartDrag = content.layoutY
            event.consume()
        }
    }

    addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
        if(event.isMiddleButtonDown){
            if(allDirections){
                content.layoutX = layoutXOnStartDrag + (event.screenX - mouseXOnStartDrag)
                content.layoutY = layoutYOnStartDrag + (event.screenY - mouseYOnStartDrag)
            }else{
                content.layoutX = minOf(layoutXOnStartDrag + (event.screenX - mouseXOnStartDrag), 0.0)
                content.layoutY = minOf(layoutYOnStartDrag + (event.screenY - mouseYOnStartDrag), 0.0)
            }
        }
    }

    addEventHandler(ScrollEvent.SCROLL) { event ->
        if(!event.isControlDown){
            if(allDirections){
                content.layoutX = content.layoutX + event.deltaX
                content.layoutY = content.layoutY + event.deltaY
            }else{
                content.layoutX = minOf(content.layoutX + event.deltaX, 0.0)
                content.layoutY = minOf(content.layoutY + event.deltaY, 0.0)
            }
        }
    }
}

val F_100: Fraction = Fraction.getFraction(100.0)

object DragAndDropType {
    val PIN = DataFormat("PIN")
}

//https://en.wikipedia.org/wiki/Field_of_view_in_video_games
// Hfov -> Vfov
fun Double.toVFov(width: Int, height: Int): Double {
    return Math.toDegrees(2.0 * Math.atan(Math.tan(Math.toRadians(this) / 2.0) * height.toDouble() / width.toDouble()))
}