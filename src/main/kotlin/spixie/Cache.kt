package spixie

import java.io.RandomAccessFile

object Cache {
    private val file = RandomAccessFile("cache", "rw")
    private var free = 0L

    fun write(byteArray: ByteArray): LongRange {
        file.seek(free)
        val start = free
        file.write(byteArray)
        free = file.filePointer
        return start until file.filePointer
    }

    fun read(longRange: LongRange): ByteArray {
        file.seek(longRange.start)
        val byteArray = ByteArray((longRange.endInclusive - longRange.start+1).toInt())
        file.read(byteArray)
        return byteArray
    }
}