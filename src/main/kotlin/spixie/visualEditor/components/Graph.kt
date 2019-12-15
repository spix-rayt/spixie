package spixie.visualEditor.components

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import spixie.Core
import spixie.arrangement.ArrangementGraphsContainer
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPin
import spixie.visualEditor.pins.ComponentPinNumber
import java.io.ObjectInput
import java.io.ObjectOutput

class Graph: Component() {
    var graph = ArrangementGraphsContainer()
        set(value) {
            field = value
            label.text = field.name.value
            field.name.changes.subscribe { newName->
                label.text = newName
            }
        }

    enum class Mode {
        Value, Sum
    }

    private val label = Label("")
    private val parameterMode = ChoiceBox<Mode>(FXCollections.observableArrayList(Mode.values().toList()))

    init {
        parameters.add(parameterMode)
        val outputPin = ComponentPinNumber("Value", null).apply {
            getValue = {
                when (parameterMode.value!!) {
                    Mode.Value -> {
                        graph.getValue(Core.arrangementWindow.visualEditor.time)
                    }
                    Mode.Sum -> {
                        graph.getSum(Core.arrangementWindow.visualEditor.time)
                    }
                }
            }
        }
        outputPins.add(outputPin)
        updateUI()
        content.children.addAll(label)
        parameterMode.selectionModel.select(0)
        parameterMode.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            Core.renderManager.requestRender()
        }
    }

    fun serialize(kryo: Kryo, output: Output) {
        output.writeDouble(layoutX)
        output.writeDouble(layoutY)
        kryo.writeObject(output, graph)
        output.writeString(parameterMode.value.name)
    }

    fun deserialize(kryo: Kryo, input: Input) {
        val x = input.readDouble()
        val y = input.readDouble()
        magneticRelocate(x, y)
        graph = kryo.readObject(input, ArrangementGraphsContainer::class.java)
        parameterMode.selectionModel.select(Mode.valueOf(input.readString()))
        updateUI()
    }
}