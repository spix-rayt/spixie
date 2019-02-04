package spixie.visualEditor.components

import org.apache.commons.lang3.math.Fraction
import spixie.NumberControl
import spixie.static.F_100
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinNumber
import spixie.visualEditor.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray
import kotlin.math.roundToInt

class ModFilter: Component(), WithParticlesArrayInput, WithParticlesArrayOutput {
    private val inParticles = ComponentPinParticleArray(this, null, "Particles")

    private val inSkip = ComponentPinNumber(this, null, "Skip", NumberControl(0.0, "").limitMin(0.0))

    private val outParticles = ComponentPinParticleArray(this, {
        val particles = inParticles.receiveValue()
        val skip = inSkip.receiveValue().roundToInt() + 1

        val resultArray = particles.array.filterIndexed { index, _ -> index%skip==0 }
        ParticleArray(resultArray, resultArray.size.toFloat())
    }, "Particles")

    init {
        inputPins.add(inParticles)
        inputPins.add(inSkip)
        outputPins.add(outParticles)
        updateVisual()
    }

    override fun getParticlesArrayInput(): ComponentPinParticleArray {
        return inParticles
    }

    override fun getParticlesArrayOutput(): ComponentPinParticleArray {
        return outParticles
    }

    companion object {
        const val serialVersionUID = 0L
    }
}