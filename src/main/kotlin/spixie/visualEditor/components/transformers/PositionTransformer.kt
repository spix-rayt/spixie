package spixie.visualEditor.components.transformers

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import spixie.static.linearInterpolate
import spixie.static.rand
import spixie.visualEditor.ParticleArray
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.roundToLong

class PositionTransformer: ParticleArrayTransformer(
        0.0,
        5.0,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        additionalParameters = arrayListOf(ChoiceBox<Axis>(FXCollections.observableArrayList(Axis.values().toList())))), Externalizable {

    enum class Axis {
        X, Y, Z
    }

    protected val parameterAxis = additionalParameters[0] as ChoiceBox<Axis>

    override fun transform(particles: ParticleArray): ParticleArray {
        val axis = parameterAxis.value
        when(parameterMode.value){
            Mode.Simple -> {
                val v = (inputSimpleValue.receiveValue() ?: 0.0).toFloat()
                when(axis){
                    Axis.X -> particles.array.forEach { it.matrix.translateLocal(v, 0.0f, 0.0f) }
                    Axis.Y -> particles.array.forEach { it.matrix.translateLocal(0.0f, v, 0.0f) }
                    Axis.Z -> particles.array.forEach { it.matrix.translateLocal(0.0f, 0.0f, v) }
                }
            }
            Mode.Linear -> {
                val first = (inputLinearFirst.receiveValue() ?: 0.0)
                val last = (inputLinearLast.receiveValue() ?: 0.0)
                particles.array.forEachIndexed { index, particle ->
                    val t = if(particles.decimalSize>1) index.toDouble()/(particles.decimalSize-1.0) else 0.0
                    val v = linearInterpolate(first, last, t).toFloat()
                    when(axis){
                        Axis.X -> particle.matrix.translateLocal(v, 0.0f, 0.0f)
                        Axis.Y -> particle.matrix.translateLocal(0.0f, v, 0.0f)
                        Axis.Z -> particle.matrix.translateLocal(0.0f, 0.0f, v)
                    }
                }
            }
            Mode.Random -> {
                val min = (inputRandomMin.receiveValue() ?: 0.0)
                val max = (inputRandomMax.receiveValue() ?: 0.0).coerceAtLeast(min)
                val seed = (inputRandomSeed.receiveValue() ?: 0.0).roundToLong()
                particles.array.forEachIndexed { index, particle ->
                    val v = (rand(0, 0, 0, 0, seed, index.toLong()) *(max - min)+min).toFloat()
                    when(axis){
                        Axis.X -> particle.matrix.translateLocal(v, 0.0f, 0.0f)
                        Axis.Y -> particle.matrix.translateLocal(0.0f, v, 0.0f)
                        Axis.Z -> particle.matrix.translateLocal(0.0f, 0.0f, v)
                    }
                }
            }
        }
        return particles
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