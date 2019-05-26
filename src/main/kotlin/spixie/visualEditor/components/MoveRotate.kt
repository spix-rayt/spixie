package spixie.visualEditor.components

import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

class MoveRotate: Component() {
    private val inParticles by lazyPinFromListOrCreate(0) { ComponentPinParticleArray("Particles") }

    private val inX by lazyPinFromListOrCreate(1) { ComponentPinNumber("X", NumberControl(0.0, "")) }

    private val inY by lazyPinFromListOrCreate(2) { ComponentPinNumber("Y", NumberControl(0.0, "")) }

    private val inZ by lazyPinFromListOrCreate(3) { ComponentPinNumber("Z", NumberControl(0.0, "")) }

    private val inRotateX by lazyPinFromListOrCreate(4) { ComponentPinNumber("RotateX", NumberControl(0.0, "")) }

    private val inRotateY by lazyPinFromListOrCreate(5) { ComponentPinNumber("RotateY", NumberControl(0.0, "")) }

    private val inRotateZ by lazyPinFromListOrCreate(6) { ComponentPinNumber("RotateZ", NumberControl(0.0, "")) }

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

    override fun creationInit() {
        inputPins.add(inParticles)
        inputPins.add(inX)
        inputPins.add(inY)
        inputPins.add(inZ)
        inputPins.add(inRotateX)
        inputPins.add(inRotateY)
        inputPins.add(inRotateZ)
    }

    override fun configInit() {
        outputPins.add(outParticles)
        updateUI()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}