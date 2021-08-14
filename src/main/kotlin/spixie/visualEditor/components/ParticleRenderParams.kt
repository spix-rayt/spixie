package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class ParticleRenderParams: Component() {
    private val inParticles = ComponentPinParticleArray("Particles")

    private val inEdge = ComponentPinNumber("Edge", NumberControl(1.0, "", -300.0).limitMin(0.0).limitMax(1.0))

    private val inTransparency = ComponentPinNumber("Transparency", NumberControl(1.0, "", -300.0).limitMin(0.0).limitMax(1.0))

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particles = inParticles.receiveValue()

            particles.forEachWithGradient { t, particle ->
                particle.edge = inEdge.receiveValue(t).toFloat()
                particle.transparency = inTransparency.receiveValue(t).toFloat()
            }
            particles
        }
    }

    init {
        inputPins.addAll(arrayListOf(inParticles, inEdge, inTransparency))
        outputPins.add(outParticles)
        updateUI()
    }
}