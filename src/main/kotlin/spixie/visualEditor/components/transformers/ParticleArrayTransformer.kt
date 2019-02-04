package spixie.visualEditor.components.transformers

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import spixie.Main
import spixie.NumberControl
import spixie.visualEditor.*
import spixie.visualEditor.components.WithParticlesArrayInput
import spixie.visualEditor.components.WithParticlesArrayOutput
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.util.concurrent.ThreadLocalRandom

abstract class ParticleArrayTransformer(val additionalParameters: ArrayList<ChoiceBox<*>> = arrayListOf()): Component(), WithParticlesArrayInput, WithParticlesArrayOutput, Externalizable {
    protected val inputParticles = ComponentPinParticleArray(this, null, "Particles")

    protected val inputFunc = ComponentPinFunc(this, null, "Func")

    private val outputParticles = ComponentPinParticleArray(this, {
        val particles = inputParticles.receiveValue()
        transform(particles)
    }, "Particles")

    init {
        val addParameters = additionalParameters
        parameters.addAll(addParameters)
        inputPins.addAll(arrayListOf(inputParticles, inputFunc))
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
        return 9
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
    }

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
    }

    companion object {
        const val serialVersionUID = 0L
    }
}