package spixie.visualEditor

import spixie.Cache
import spixie.static.MAGIC
import spixie.static.mix
import java.nio.ByteBuffer

class ParticleArray {
    val array: List<Particle> by lazy { lazyArray() }
    val hash: Long by lazy { lazyHash() }
    val decimalSize:Float by lazy { lazySize() }
    private val lazyArray: () -> List<Particle>
    private val lazyHash: () -> Long
    private val lazySize: () -> Float

    constructor(array: List<Particle>, size: Float) {
        lazyArray = { array }
        lazyHash = {
            array.fold(MAGIC.toLong()) { acc, particle ->
                acc mix particle.spixieHash()
            }
        }
        lazySize = { size }
    }

    constructor(hash: Long){
        val (particles, size) = loadFromCache()
        lazyArray = { particles }
        lazyHash = { hash }
        lazySize = { size }
    }

    fun saveInCache(){
        if(!cacheMap.containsKey(hash)){
            val bytes = ByteBuffer.allocate(array.size * Particle.PARTICLE_FLOATS * 4)
            val floats = bytes.asFloatBuffer()
            floats.put(decimalSize)
            array.forEach {
                it.saveTo(floats)
            }
            cacheMap[hash] = Cache.write(bytes.array())
        }
    }

    fun loadFromCache(): Pair<List<Particle>, Float>{
        cacheMap.getOrElse(hash, { return listOf<Particle>() to 0.0f }).let {
            val floats = ByteBuffer.wrap(Cache.read(it)).asFloatBuffer()
            val count = (floats.capacity() - 1) / Particle.PARTICLE_FLOATS
            val size = floats.get()
            val a = ArrayList<Particle>(count)
            for (i in 0 until count){
                a.add(Particle().apply { loadFrom(floats) })
            }
            return a to size
        }
    }

    companion object {
        val cacheMap = hashMapOf<Long, LongRange>()
    }
}