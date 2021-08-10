package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.EditorComponent
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class MoveRotate: EditorComponent() {
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
            val mx = inX.receiveValue()
            val my = inY.receiveValue()
            val mz = inZ.receiveValue()
            val rx = inRotateX.receiveValue()
            val ry = inRotateY.receiveValue()
            val rz = inRotateZ.receiveValue()

            particles.array.forEach {
                it.apply {
                    matrix
                            .rotateLocalX((rx * Math.PI * 2).toFloat())
                            .rotateLocalY((ry * Math.PI * 2).toFloat())
                            .rotateLocalZ((rz * Math.PI * 2).toFloat())
                            .translateLocal(mx.toFloat(), my.toFloat(), mz.toFloat())
                }
            }
            particles
        }
    }

    fun changeZ(value:Double){
        inZ.valueControl?.value = value
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