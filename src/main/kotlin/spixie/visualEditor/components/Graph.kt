package spixie.visualEditor.components

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import spixie.renderManager
import spixie.timeline.ArrangementGraphsContainer
import spixie.visualEditor
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber

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
    private val parameterMode = ChoiceBox(FXCollections.observableArrayList(Mode.values().toList()))

    init {
        parameters.add(parameterMode)
        val outputPin = ComponentPinNumber("Value", null).apply {
            getValue = {
                when (parameterMode.value!!) {
                    Mode.Value -> {
                        graph.getValue(visualEditor.beats)
                    }
                    Mode.Sum -> {
                        graph.getSum(visualEditor.beats)
                    }
                }
            }
        }
        outputPins.add(outputPin)
        updateUI()
        content.children.addAll(label)
        parameterMode.selectionModel.select(0)
        parameterMode.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            renderManager.requestRender()
        }
    }
}