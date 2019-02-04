package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinNumber
import spixie.visualEditor.ComponentPinParticleArray

class MoveRotate: Component(), WithParticlesArrayInput, WithParticlesArrayOutput {
    private val inParticles = ComponentPinParticleArray(this, null, "Particles")

    private val inX = ComponentPinNumber(this, null, "X", NumberControl(0.0, ""))

    private val inY = ComponentPinNumber(this, null, "Y", NumberControl(0.0, ""))

    private val inZ = ComponentPinNumber(this, null, "Z", NumberControl(0.0, ""))

    private val inRotateX = ComponentPinNumber(this, null, "RotateX", NumberControl(0.0, ""))

    private val inRotateY = ComponentPinNumber(this, null, "RotateY", NumberControl(0.0, ""))

    private val inRotateZ = ComponentPinNumber(this, null, "RotateZ", NumberControl(0.0, ""))

    private val outParticles = ComponentPinParticleArray(this, {
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
                        .rotateLocalX((rx * Math.PI*2).toFloat())
                        .rotateLocalY((ry * Math.PI*2).toFloat())
                        .rotateLocalZ((rz * Math.PI*2).toFloat())
                        .translateLocal(mx.toFloat(), my.toFloat(), mz.toFloat())
            }
        }
        particles
    }, "Particles")

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