package spixie.visual_editor.components

import org.joml.*
import spixie.visual_editor.Particle
import spixie.visual_editor.Component
import spixie.visual_editor.ComponentPin
import spixie.visual_editor.ParticleArray
import java.lang.Math

class Test: Component() {
    val inParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java)
    val inLength = ComponentPin(this, null, "Length",Double::class.java)
    val inRotateX = ComponentPin(this, null, "RotateX", Double::class.java)
    val inRotateY = ComponentPin(this, null, "RotateY", Double::class.java)
    val inRotateZ = ComponentPin(this, null, "RotateZ", Double::class.java)
    val inSize = ComponentPin(this, null, "Size", Double::class.java)
    val inCount = ComponentPin(this, null, "Count", Double::class.java)


    val outParticles = ComponentPin(this, {
        val p = inParticles.receiveValue() ?: ParticleArray(arrayListOf(Particle()))
        val len = inLength.receiveValue() ?: 0.0
        val rx = inRotateX.receiveValue() ?: 0.0
        val ry = inRotateY.receiveValue() ?: 0.0
        val rz = inRotateZ.receiveValue() ?: 0.0
        val size = inSize.receiveValue() ?: 0.0
        val count = inCount.receiveValue()?.toInt() ?: 0

        ParticleArray(
                (0 until count).flatMap { i ->
                    p.array.map { pa ->
                        Particle().apply {
                            matrix.set(pa.matrix)
                                    .scaleLocal(size.toFloat())
                                    .rotateLocalX((rx * Math.PI*2).toFloat())
                                    .rotateLocalY((ry * Math.PI*2).toFloat())
                                    .rotateLocalZ((rz * Math.PI*2).toFloat())
                                    .translateLocal(((if(count>1) (i * (len / (count-1))) else 0.0) - len / 2.0).toFloat(), 0.0f, 0.0f)
                            this.size = size.toFloat()*pa.size
                        }
                    }
                }
        )
    }, "Particles", ParticleArray::class.java)

    init {
        inputPins.add(inParticles)
        inputPins.add(inLength)
        inputPins.add(inRotateX)
        inputPins.add(inRotateY)
        inputPins.add(inRotateZ)
        inputPins.add(inSize)
        inputPins.add(inCount)
        outputPins.add(outParticles)
        updateVisual()
    }
}