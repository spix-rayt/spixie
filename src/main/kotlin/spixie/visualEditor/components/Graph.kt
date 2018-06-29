package spixie.visualEditor.components

import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import spixie.ArrangementGraphsContainer
import spixie.Main
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPinNumber
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
        outputPins.add(ComponentPinNumber(this, {
            when(parameterMode.value){
                Mode.Value -> {
                    graph.getValue(Main.arrangementWindow.visualEditor.time)
                }
                Mode.Sum -> {
                    graph.getSum(Main.arrangementWindow.visualEditor.time)
                }
            }
        }, "Value", null))
        updateVisual()
        content.children.addAll(label)
        parameterMode.selectionModel.select(0)
        parameterMode.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            Main.renderManager.requestRender()
        }
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
        parameterMode.selectionModel.select(Graph.Mode.valueOf(o.readUTF()))
    }

    companion object {
        const val serialVersionUID = 0L
    }
}