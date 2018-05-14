package spixie.visualEditor

import spixie.Cache
import spixie.static.MAGIC
import spixie.static.mix
import java.nio.ByteBuffer

class ParticleArray {
    val array: List<Particle> by lazy { lazyArray() }
    val hash: Long
    private val lazyArray: () -> List<Particle>

    constructor(array: List<Particle>) {
        lazyArray = { array }
        hash = array.fold(MAGIC.toLong()) { acc, particle ->
            acc mix particle.spixieHash()
        }
    }

    constructor(hash: Long){
        this.hash = hash
        lazyArray = {
            loadFromCache()
        }
    }

    fun saveInCache(){
        if(!cacheMap.containsKey(hash)){
            val bytes = ByteBuffer.allocate(array.size * Particle.PARTICLE_FLOATS * 4)
            val floats = bytes.asFloatBuffer()
            array.forEach {
                it.saveTo(floats)
            }
            cacheMap[hash] = Cache.write(bytes.array())
        }
    }

    fun loadFromCache(): List<Particle>{
        cacheMap.getOrElse(hash, { return listOf() }).let {
            val floats = ByteBuffer.wrap(Cache.read(it)).asFloatBuffer()
            val size = floats.capacity() / Particle.PARTICLE_FLOATS
            val a = ArrayList<Particle>(size)
            for (i in 0 until size){
                a.add(Particle().apply { loadFrom(floats) })
            }
            return a
        }
    }

    companion object {
        val cacheMap = hashMapOf<Long, LongRange>()
    }
}