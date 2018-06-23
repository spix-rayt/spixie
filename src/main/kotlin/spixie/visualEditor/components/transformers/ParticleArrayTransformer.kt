package spixie.visualEditor.components.transformers

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import spixie.Main
import spixie.NumberControl
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinNumber
import spixie.visualEditor.ComponentPinParticleArray
import spixie.visualEditor.ParticleArray
import spixie.visualEditor.components.WithParticlesArrayInput
import spixie.visualEditor.components.WithParticlesArrayOutput
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.util.concurrent.ThreadLocalRandom

abstract class ParticleArrayTransformer(default: Double, dragDelta: Double, val min: Double, val max:Double, val additionalParameters: ArrayList<ChoiceBox<*>> = arrayListOf()): Component(), WithParticlesArrayInput, WithParticlesArrayOutput, Externalizable {
    enum class Mode {
        Simple, Linear, Random
    }
    protected val inputParticles = ComponentPinParticleArray(this, null, "Particles")

    protected val inputSimpleValue = ComponentPinNumber(this, null, "Value", NumberControl(default, dragDelta, "").limitMin(min).limitMax(max))

    protected val inputLinearFirst = ComponentPinNumber(this, null, "First", NumberControl(default, dragDelta, "").limitMin(min).limitMax(max))
    protected val inputLinearLast = ComponentPinNumber(this, null, "Last", NumberControl(default, dragDelta, "").limitMin(min).limitMax(max))

    protected val inputRandomMin = ComponentPinNumber(this, null, "Min", NumberControl((0.0).coerceIn(min, max), dragDelta, "").limitMin(min).limitMax(max))
    protected val inputRandomMax = ComponentPinNumber(this, null, "Max", NumberControl(default, dragDelta, "").limitMin(min).limitMax(max))
    protected val inputRandomSeed = ComponentPinNumber(this, null, "Seed", NumberControl(ThreadLocalRandom.current().nextInt(0, 10000).toDouble(), 1.0, "").limitMin(0.0))

    protected val parameterMode = ChoiceBox<Mode>(FXCollections.observableArrayList(Mode.values().toList())).apply {
        selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            inputPins.forEach { it.isVisible = false }
            inputParticles.isVisible = true
            when(newValue!!){
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

    private val outputParticles = ComponentPinParticleArray(this, {
        val particles = inputParticles.receiveValue()
        transform(particles)
    }, "Particles")

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

    override fun getHeightInCells(): Int {
        return 7
    }

    abstract fun transform(particles: ParticleArray): ParticleArray

    override fun getParticlesArrayInput(): ComponentPinParticleArray {
        return inputParticles
    }

    override fun getParticlesArrayOutput(): ComponentPinParticleArray {
        return outputParticles
    }

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