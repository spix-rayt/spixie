package spixie.visualEditor.components

import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ParticleArray

class MoveRotate: Component() {
    private val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)
    private val inX = ComponentPin(this, null, "X",Double::class.java, ValueControl(0.0, 5.00, ""))
    private val inY = ComponentPin(this, null, "Y",Double::class.java, ValueControl(0.0, 5.00, ""))
    private val inZ = ComponentPin(this, null, "Z",Double::class.java, ValueControl(0.0, 5.00, ""))
    private val inRotateX = ComponentPin(this, null, "RotateX", Double::class.java, ValueControl(0.0, 0.01, ""))
    private val inRotateY = ComponentPin(this, null, "RotateY", Double::class.java, ValueControl(0.0, 0.01, ""))
    private val inRotateZ = ComponentPin(this, null, "RotateZ", Double::class.java, ValueControl(0.0, 0.01, ""))


    private val outParticles = ComponentPin(this, {
        val particles = inParticles.receiveValue() ?: ParticleArray(arrayListOf(), 0.0f)
        val mx = inX.receiveValue() ?: 0.0
        val my = inY.receiveValue() ?: 0.0
        val mz = inZ.receiveValue() ?: 0.0
        val rx = inRotateX.receiveValue() ?: 0.0
        val ry = inRotateY.receiveValue() ?: 0.0
        val rz = inRotateZ.receiveValue() ?: 0.0

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
    }, "Particles", ParticleArray::class.java, null)

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

    companion object {
        const val serialVersionUID = 0L
    }
}