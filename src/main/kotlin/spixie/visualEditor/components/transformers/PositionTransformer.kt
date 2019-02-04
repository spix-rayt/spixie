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

class PositionTransformer: ParticleArrayTransformer(
        additionalParameters = arrayListOf(ChoiceBox<Axis>(FXCollections.observableArrayList(Axis.values().toList())))), Externalizable {

    enum class Axis {
        X, Y, Z
    }

    protected val parameterAxis = additionalParameters[0] as ChoiceBox<Axis>

    override fun transform(particles: ParticleArray): ParticleArray {
        val axis = parameterAxis.value!!

        particles.array.forEachIndexed { index, particle ->
            val t = if (particles.decimalSize > 1) index.toDouble() / (particles.decimalSize - 1.0) else 0.0
            val v = inputFunc.receiveValue(t).toFloat()
            when(axis){
                Axis.X -> particle.matrix.translateLocal(v, 0.0f, 0.0f)
                Axis.Y -> particle.matrix.translateLocal(0.0f, v, 0.0f)
                Axis.Z -> particle.matrix.translateLocal(0.0f, 0.0f, v)
            }
        }
        return particles
    }

    override fun getHeightInCells(): Int {
        return 10
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