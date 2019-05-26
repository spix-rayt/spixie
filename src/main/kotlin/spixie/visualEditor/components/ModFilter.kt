package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray
import kotlin.math.roundToInt

class ModFilter: Component() {
    private val inParticles by lazyPinFromListOrCreate(0) { ComponentPinParticleArray("Particles") }

    private val inSkip by lazyPinFromListOrCreate(1) { ComponentPinNumber("Skip", NumberControl(0.0, "").limitMin(0.0)) }

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particles = inParticles.receiveValue()
            val skip = inSkip.receiveValue().roundToInt() + 1

            val resultArray = particles.array.filterIndexed { index, _ -> index % skip == 0 }
            ParticleArray(resultArray, resultArray.size.toFloat())
        }
    }

    override fun creationInit() {
        inputPins.add(inParticles)
        inputPins.add(inSkip)
    }

    override fun configInit() {
        outputPins.add(outParticles)
        updateUI()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}