package spixie.visualEditor.components

import spixie.ValueControl
import spixie.static.linearInterpolate
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.Particle
import spixie.visualEditor.ParticleArray

class SimpleParticlesGenerator: Component() {
    private val inStart = ComponentPin(this, null, "Start",Double::class.java, ValueControl(0.0, 5.00, ""))
    private val inEnd = ComponentPin(this, null, "End",Double::class.java, ValueControl(0.0, 5.00, ""))
    private val inRotateXFirst = ComponentPin(this, null, "RotateXFirst", Double::class.java, ValueControl(0.0, 0.01, ""))
    private val inRotateXLast = ComponentPin(this, null, "RotateXLast", Double::class.java, ValueControl(0.0, 0.01, ""))
    private val inRotateYFirst = ComponentPin(this, null, "RotateYFirst", Double::class.java, ValueControl(0.0, 0.01, ""))
    private val inRotateYLast = ComponentPin(this, null, "RotateYLast", Double::class.java, ValueControl(0.0, 0.01, ""))
    private val inRotateZFirst = ComponentPin(this, null, "RotateZFirst", Double::class.java, ValueControl(0.0, 0.01, ""))
    private val inRotateZLast = ComponentPin(this, null, "RotateZLast", Double::class.java, ValueControl(0.0, 0.01, ""))
    private val inScaleFirst = ComponentPin(this, null, "ScaleFirst", Double::class.java, ValueControl(1.0, 0.1, "").limitMin(0.0))
    private val inScaleLast = ComponentPin(this, null, "ScaleLast", Double::class.java, ValueControl(1.0, 0.1, "").limitMin(0.0))
    private val inCount = ComponentPin(this, null, "Count", Double::class.java, ValueControl(1.0, 0.1, "").limitMin(0.0))


    private val outParticles = ComponentPin(this, {
        val start = inStart.receiveValue() ?: 0.0
        val end = inEnd.receiveValue() ?: 0.0
        val rxf = inRotateXFirst.receiveValue() ?: 0.0
        val rxl = inRotateXLast.receiveValue() ?: 0.0
        val ryf = inRotateYFirst.receiveValue() ?: 0.0
        val ryl = inRotateYLast.receiveValue() ?: 0.0
        val rzf = inRotateZFirst.receiveValue() ?: 0.0
        val rzl = inRotateZLast.receiveValue() ?: 0.0
        val scaleFirst = inScaleFirst.receiveValue() ?: 0.0
        val scaleLast = inScaleLast.receiveValue() ?: 0.0
        val count = inCount.receiveValue() ?: 0.0

        val resultArray = (0 until count.toInt()).map { i ->
            Particle().apply {
                val t = if(count>1) i.toDouble()/(count-1) else 0.0
                val currentScale = linearInterpolate(scaleFirst, scaleLast, t).toFloat()
                val currentRotateX = linearInterpolate(rxf, rxl, t).toFloat()
                val currentRotateY = linearInterpolate(ryf, ryl, t).toFloat()
                val currentRotateZ = linearInterpolate(rzf, rzl, t).toFloat()
                matrix
                        .scaleLocal(currentScale)
                        .rotateLocalX((currentRotateX * Math.PI*2).toFloat())
                        .rotateLocalY((currentRotateY * Math.PI*2).toFloat())
                        .rotateLocalZ((currentRotateZ * Math.PI*2).toFloat())
                        .translateLocal(linearInterpolate(start, end, t).toFloat(), 0.0f, 0.0f)
            }
        }

        ParticleArray(resultArray, count.toFloat())
    }, "Particles", ParticleArray::class.java, null)

    init {
        inputPins.add(inStart)
        inputPins.add(inEnd)
        inputPins.add(inRotateXFirst)
        inputPins.add(inRotateXLast)
        inputPins.add(inRotateYFirst)
        inputPins.add(inRotateYLast)
        inputPins.add(inRotateZFirst)
        inputPins.add(inRotateZLast)
        inputPins.add(inScaleFirst)
        inputPins.add(inScaleLast)
        inputPins.add(inCount)
        outputPins.add(outParticles)
        updateVisual()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}