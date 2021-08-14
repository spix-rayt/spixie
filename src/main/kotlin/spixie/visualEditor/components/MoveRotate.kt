package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class MoveRotate: Component() {
    private val inParticles = ComponentPinParticleArray("Particles")

    private val inX = ComponentPinNumber("X", NumberControl(0.0, ""))

    private val inY = ComponentPinNumber("Y", NumberControl(0.0, ""))

    private val inZ = ComponentPinNumber("Z", NumberControl(0.0, ""))

    private val inRotateX = ComponentPinNumber("RotateX", NumberControl(0.0, ""))

    private val inRotateY = ComponentPinNumber("RotateY", NumberControl(0.0, ""))

    private val inRotateZ = ComponentPinNumber("RotateZ", NumberControl(0.0, ""))

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particles = inParticles.receiveValue()

            particles.forEachWithGradient { t, particle ->
                val mx = inX.receiveValue(t)
                val my = inY.receiveValue(t)
                val mz = inZ.receiveValue(t)
                val rx = inRotateX.receiveValue(t)
                val ry = inRotateY.receiveValue(t)
                val rz = inRotateZ.receiveValue(t)

                particle.matrix
                    .rotateLocalX((rx * Math.PI * 2).toFloat())
                    .rotateLocalY((ry * Math.PI * 2).toFloat())
                    .rotateLocalZ((rz * Math.PI * 2).toFloat())
                    .translateLocal(mx.toFloat(), my.toFloat(), mz.toFloat())
            }
            particles
        }
    }

    init {
        inputPins.add(inParticles)
        inputPins.add(inX)
        inputPins.add(inY)
        inputPins.add(inZ)
        inputPins.add(inRotateX)
        inputPins.add(inRotateY)
        inputPins.add(inRotateZ)
        outputPins.add(outParticles)
        updateUI()
    }
}