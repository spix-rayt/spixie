package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.*

class SimpleParticlesGenerator: Component(), WithParticlesArrayOutput {
    private val inCount = ComponentPinNumber(this, null, "Count", NumberControl(1.0, "").limitMin(0.0))

    private val outParticles = ComponentPinParticleArray(this, {
        val count = inCount.receiveValue()

        ParticleArray(Array(count.toInt()) { Particle() }.toList(), count.toFloat())
    }, "Particles")

    init {
        inputPins.add(inCount)
        outputPins.add(outParticles)
        updateVisual()
    }

    override fun getParticlesArrayOutput(): ComponentPinParticleArray {
        return outParticles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}