package spixie.static

import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.input.DataFormat
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import org.apache.commons.lang3.math.Fraction
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO

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

fun BufferedImage.toPNGByteArray():ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(this, "png", byteArrayOutputStream)
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

fun Pane.initCustomPanning(content:Group, allDirections: Boolean){
    var mouseXOnStartDrag = 0.0
    var mouseYOnStartDrag = 0.0
    var layoutXOnStartDrag = 0.0
    var layoutYOnStartDrag = 0.0

    setOnMousePressed { event ->
        if(event.button == MouseButton.MIDDLE){
            mouseXOnStartDrag = event.screenX
            mouseYOnStartDrag = event.screenY
            layoutXOnStartDrag = content.layoutX
            layoutYOnStartDrag = content.layoutY
        }
    }

    addEventFilter(MouseEvent.MOUSE_DRAGGED, { event ->
        if(event.isMiddleButtonDown){
            if(allDirections){
                content.layoutX = layoutXOnStartDrag + (event.screenX - mouseXOnStartDrag)
                content.layoutY = layoutYOnStartDrag + (event.screenY - mouseYOnStartDrag)
            }else{
                content.layoutX = minOf(layoutXOnStartDrag + (event.screenX - mouseXOnStartDrag), 0.0)
                content.layoutY = minOf(layoutYOnStartDrag + (event.screenY - mouseYOnStartDrag), 0.0)
            }
        }
    })
}

val F_100 = Fraction.getFraction(100.0)

object DragAndDropType {
    val PIN = DataFormat("PIN")
}