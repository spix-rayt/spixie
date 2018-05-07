package spixie.static

import javafx.application.Platform
import javafx.scene.input.DataFormat
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
    return ((x % 0xFFFFFFFFFFFFFFF).toFloat()/0xFFFFFFFFFFFFFFF);
}

fun frameToTime(frame:Int, bpm:Double): Double {
    return bpm/3600*frame
}

val magic = 0x5bd1e995
infix fun Long.mix(n:Long):Long{
    var q1 = this * magic
    q1 = q1 xor (q1 ushr 48)
    q1 *= magic
    var q2 = n * magic
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
    var r = this % multiplicity
    if(r == 0){
        return this
    }else{
        return this + multiplicity - r
    }
}

fun linearInterpolate(y1: Double, y2:Double, t:Double): Double {
    return (y1*(1.0-t)+y2*t);
}

object DragAndDropType {
    val INTERNALOBJECT = DataFormat("INTERNALOBJECT")
}