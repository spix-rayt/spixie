package spixie.visualEditor.components

import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinParticleArray
import spixie.visualEditor.Module
import spixie.visualEditor.ParticleArray
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class ModuleComponent: Component(), Externalizable {
    var module: Module? = null
    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            module?.findParticlesResultComponent()?.getParticles() ?: ParticleArray(arrayListOf(), 0.0f)
        }
    }

    override fun configInit() {
        outputPins.add(outParticles)
        updateUI()
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
        const val serialVersionUID = 0L
    }
}