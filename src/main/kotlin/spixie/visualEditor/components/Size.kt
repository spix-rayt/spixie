package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class Size: Component() {
    private val inParticles = ComponentPinParticleArray("Particles")

    private val inScale = ComponentPinNumber("Scale", NumberControl(1.0, "", -300.0).limitMin(0.0))

    private val inSize = ComponentPinNumber("Size", NumberControl(1.0, "", -300.0).limitMin(0.0))

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particles = inParticles.receiveValue()

            particles.forEachWithGradient { t, particle ->
                particle.matrix.scaleLocal(inScale.receiveValue(t).toFloat())
                particle.size = inSize.receiveValue(t).toFloat()
            }
            particles
        }
    }

    init {
        inputPins.addAll(arrayListOf(inParticles, inScale, inSize))
        outputPins.add(outParticles)
        updateUI()
    }
}