package spixie.visualEditor.components

import javafx.scene.control.Label
import spixie.ArrangementGraphsContainer
import spixie.Main
import spixie.visualEditor.Component
import spixie.visualEditor.ComponentPin
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

    private val label = Label("")

    init {
        outputPins.add(ComponentPin(this, {
            graph.getValue(Main.arrangementWindow.visualEditor.time)
        }, "Value", Double::class.java, null))
        updateVisual()
        content.children.addAll(label)
    }

    override fun writeExternal(o: ObjectOutput) {
        super.writeExternal(o)
        o.writeObject(graph)
    }

    override fun readExternal(o: ObjectInput) {
        super.readExternal(o)
        graph = o.readObject() as ArrangementGraphsContainer
        label.text = graph.name.value
        graph.name.changes.subscribe { newName->
            label.text = newName
        }
    }

    companion object {
        const val serialVersionUID = 0L
    }
}