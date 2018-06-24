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
                val v = (inputValue.receiveValue() * Math.PI * 2).toFloat()
                when(axis){
                    Axis.X -> particles.array.forEach { it.matrix.rotateLocalX(v) }
                    Axis.Y -> particles.array.forEach { it.matrix.rotateLocalY(v) }
                    Axis.Z -> particles.array.forEach { it.matrix.rotateLocalZ(v) }
                }
            }
            Mode.Linear -> {
                val first = inputFirst.receiveValue()
                val last = inputLast.receiveValue()
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
                val min = inputMin.receiveValue()
                val max = inputMax.receiveValue().coerceAtLeast(min)
                val offset = inputOffset.receiveValue()
                val stretch = inputStretch.receiveValue()
                val seed = inputSeed.receiveValue()
                particles.array.forEachIndexed { index, particle ->
                    val i = ((index+offset) / stretch)
                    val leftRandom  = rand(0, 0, 0, 0, seed.toLong(), i.toLong()).toDouble()
                    val rightRandom = rand(0, 0, 0, 0, seed.toLong(), i.toLong()+1L).toDouble()
                    val rand = perlinInterpolate(leftRandom, rightRandom, i%1)
                    val leftRandom2  = rand(0, 0, 0, 0, seed.toLong()+1, i.toLong()).toDouble()
                    val rightRandom2 = rand(0, 0, 0, 0, seed.toLong()+1, i.toLong()+1L).toDouble()
                    val rand2 = perlinInterpolate(leftRandom2, rightRandom2, i%1)
                    val finalRand = linearInterpolate(rand, rand2, seed%1)
                    val v = ((finalRand *(max - min)+min) * Math.PI * 2).toFloat()
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