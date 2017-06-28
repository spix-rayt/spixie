package spixie

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