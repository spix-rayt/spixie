package spixie.visualEditor.components

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import spixie.Core
import spixie.arrangement.ArrangementGraphsContainer
import spixie.visualEditor.Component
import spixie.visualEditor.pins.ComponentPinNumber
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class Graph(): Component(), Externalizable {
    private lateinit var graph: ArrangementGraphsContainer

    constructor(graph: ArrangementGraphsContainer): this(){
        this.graph = graph
        label.text = graph.name.value
        graph.name.changes.subscribe { newName->
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
        outputPins.add(ComponentPinNumber("Value", null).apply {
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
        })
        updateUI()
        content.children.addAll(label)
        parameterMode.selectionModel.select(0)
        parameterMode.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            Core.renderManager.requestRender()
        }
    }

    override fun creationInit() {
        //TODO
    }

    override fun configInit() {

    }

    override fun writeExternal(o: ObjectOutput) {
        super.writeExternal(o)
        o.writeObject(graph)
        o.writeUTF(parameterMode.value.name)
    }

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
        graph = o.readObject() as ArrangementGraphsContainer
        label.text = graph.name.value
        graph.name.changes.subscribe { newName->
            label.text = newName
        }
        parameterMode.selectionModel.select(Mode.valueOf(o.readUTF()))
    }

    companion object {
        const val serialVersionUID = 0L
    }
}