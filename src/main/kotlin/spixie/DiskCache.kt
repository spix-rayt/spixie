package spixie

import java.io.RandomAccessFile

object DiskCache {
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
        file.seek(longRange.first)
        val byteArray = ByteArray((longRange.last - longRange.first + 1).toInt())
        file.read(byteArray)
        return byteArray
    }
}