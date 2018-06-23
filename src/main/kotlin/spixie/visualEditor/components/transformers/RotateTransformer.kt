package spixie.visualEditor.components.transformers

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import spixie.static.linearInterpolate
import spixie.static.perlinInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.roundToLong

class RotateTransformer: ParticleArrayTransformer(
        0.0,
        0.001,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        additionalParameters = arrayListOf(ChoiceBox<Axis>(FXCollections.observableArrayList(Axis.values().toList())))), Externalizable {

    enum class Axis {
        X, Y, Z
    }

    protected val parameterAxis = additionalParameters[0] as ChoiceBox<Axis>

    override fun transform(particles: ParticleArray): ParticleArray {
        val axis = parameterAxis.value!!
        when(parameterMode.value!!){
            Mode.Simple -> {
                val v = (inputSimpleValue.receiveValue() * Math.PI * 2).toFloat()
                when(axis){
                    Axis.X -> particles.array.forEach { it.matrix.rotateLocalX(v) }
                    Axis.Y -> particles.array.forEach { it.matrix.rotateLocalY(v) }
                    Axis.Z -> particles.array.forEach { it.matrix.rotateLocalZ(v) }
                }
            }
            Mode.Linear -> {
                val first = inputLinearFirst.receiveValue()
                val last = inputLinearLast.receiveValue()
                particles.array.forEachIndexed { index, particle ->
                    val t = if(particles.decimalSize>1) index.toDouble()/(particles.decimalSize-1.0) else 0.0
                    val v = (linearInterpolate(first, last, t) * Math.PI * 2).toFloat()
                    when(axis){
                        Axis.X -> particle.matrix.rotateLocalX(v)
                        Axis.Y -> particle.matrix.rotateLocalY(v)
                        Axis.Z -> particle.matrix.rotateLocalZ(v)
                    }
                }
            }
            Mode.Random -> {
                val min = inputRandomMin.receiveValue()
                val max = inputRandomMax.receiveValue().coerceAtLeast(min)
                val stretch = inputRandomStretch.receiveValue()
                val seed = inputRandomSeed.receiveValue().roundToLong()
                particles.array.forEachIndexed { index, particle ->
                    val i = (index / stretch)
                    val leftRandom  = rand(0, 0, 0, 0, seed, i.toLong()).toDouble()
                    val rightRandom = rand(0, 0, 0, 0, seed, i.toLong()+1L).toDouble()
                    val rand = perlinInterpolate(leftRandom, rightRandom, i%1)
                    val v = ((rand *(max - min)+min) * Math.PI * 2).toFloat()
                    when(axis){
                        Axis.X -> particle.matrix.rotateLocalX(v)
                        Axis.Y -> particle.matrix.rotateLocalY(v)
                        Axis.Z -> particle.matrix.rotateLocalZ(v)
                    }
                }
            }
        }
        return particles
    }

    override fun getHeightInCells(): Int {
        return 9
    }

    override fun writeExternal(o: ObjectOutput) {
        super.writeExternal(o)
        o.writeUTF(parameterAxis.value.name)
    }

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
        parameterAxis.selectionModel.select(Axis.valueOf(o.readUTF()))
    }

    companion object {
        const val serialVersionUID = 0L
    }
}