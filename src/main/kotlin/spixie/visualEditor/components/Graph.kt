package spixie.visualEditor.components

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import spixie.Core
import spixie.arrangement.ArrangementGraphsContainer
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
            GlobalScope.launch {
                Core.renderManager.requestRender()
            }
        }
    }
}