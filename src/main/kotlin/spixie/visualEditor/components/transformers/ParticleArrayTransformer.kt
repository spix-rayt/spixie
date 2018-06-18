package spixie.visualEditor.components.transformers

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import spixie.Main
import spixie.ValueControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
import spixie.visualEditor.ParticleArray
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.util.concurrent.ThreadLocalRandom

abstract class ParticleArrayTransformer(val default: Double, val dragDelta: Double, val min: Double, val max:Double, val additionalParameters: ArrayList<ChoiceBox<*>> = arrayListOf()): Component(), Externalizable {
    enum class Mode {
        Simple, Linear, Random
    }
    protected val inputParticles = ComponentPin(this, null, "Particles", ParticleArray::class.java, null)

    protected val inputSimpleValue = ComponentPin(this, null, "Value", Double::class.java, ValueControl(default, dragDelta, "").limitMin(min).limitMax(max))

    protected val inputLinearFirst = ComponentPin(this, null, "First", Double::class.java, ValueControl(default, dragDelta, "").limitMin(min).limitMax(max))
    protected val inputLinearLast = ComponentPin(this, null, "Last", Double::class.java, ValueControl(default, dragDelta, "").limitMin(min).limitMax(max))

    protected val inputRandomMin = ComponentPin(this, null, "Min", Double::class.java, ValueControl((0.0).coerceIn(min, max), dragDelta, "").limitMin(min).limitMax(max))
    protected val inputRandomMax = ComponentPin(this, null, "Max", Double::class.java, ValueControl(default, dragDelta, "").limitMin(min).limitMax(max))
    protected val inputRandomSeed = ComponentPin(this, null, "Seed", Double::class.java, ValueControl(ThreadLocalRandom.current().nextInt(0, 10000).toDouble(), 1.0, "").limitMin(0.0))

    protected val parameterMode = ChoiceBox<Mode>(FXCollections.observableArrayList(Mode.values().toList())).apply {
        selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            inputPins.forEach { it.isVisible = false }
            inputParticles.isVisible = true
            when(newValue){
                Mode.Simple -> {
                    inputSimpleValue.isVisible = true
                }
                Mode.Linear -> {
                    inputLinearFirst.isVisible = true
                    inputLinearLast.isVisible = true
                }
                Mode.Random -> {
                    inputRandomMin.isVisible = true
                    inputRandomMax.isVisible = true
                    inputRandomSeed.isVisible = true
                }
            }
            inputPins.filterNot { it.isVisible }.forEach { this@ParticleArrayTransformer.disconnectPinRequest.onNext(it) }
            updateVisual()
        }
    }

    private val outputParticles = ComponentPin(this, {
        val particles = inputParticles.receiveValue() ?: ParticleArray(arrayListOf(), 0.0f)
        transform(particles)
    }, "Particles", ParticleArray::class.java, null)

    init {
        val addParameters = additionalParameters + arrayListOf(parameterMode)
        parameters.addAll(addParameters)
        inputPins.addAll(arrayListOf(inputParticles, inputSimpleValue, inputLinearFirst, inputLinearLast, inputRandomMin, inputRandomMax, inputRandomSeed))
        outputPins.add(outputParticles)
        updateVisual()
        addParameters.forEach {
            it.selectionModel.select(0)
            it.selectionModel.selectedItemProperty().addListener { _, _, _ ->
                Main.renderManager.requestRender()
            }
        }
    }

    abstract fun transform(particles: ParticleArray): ParticleArray

    override fun writeExternal(o: ObjectOutput) {
        super.writeExternal(o)
        o.writeUTF(parameterMode.value.name)
    }

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
        parameterMode.selectionModel.select(Mode.valueOf(o.readUTF()))
    }

    companion object {
        const val serialVersionUID = 0L
    }
}