package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinParticleArray
import spixie.visualEditor.Module
import spixie.visualEditor.ParticleArray
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class ModuleComponent: Component(), Externalizable {
    var module: Module? = null
    private val outParticles = ComponentPinParticleArray(this, {
        /*module?.let { module ->
            val cacheKey = module.calcHashOfConsts()
            val hash = cache[cacheKey]
            if(hash != null){
                if(ParticleArray.cacheMap.containsKey(hash)){
                    return@ComponentPin ParticleArray(hash)
                }
            }

            val result = module.findResultComponent().getParticles()
            result.saveInCache()
            cache[cacheKey] = result.hash
            return@ComponentPin result
        }*/
        ParticleArray(arrayListOf(), 0.0f)
    }, "Particles")

    init {
        outputPins.add(outParticles)
        updateVisual()
    }

    override fun writeExternal(o: ObjectOutput) {
        super.writeExternal(o)
        o.writeUTF(module?.name ?: "")
    }

    var externalName = ""
        private set

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
        externalName = o.readUTF()
    }

    companion object {
        val cache = hashMapOf<Long, Long>()
        const val serialVersionUID = 0L
    }
}