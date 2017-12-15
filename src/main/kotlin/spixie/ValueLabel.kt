package spixie

import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.*
import javafx.scene.layout.HBox
import org.apache.commons.math3.fraction.Fraction
import spixie.components.Graph

class ValueLabel(private val name: String) : HBox() {
    private val labelName = Label()
    private val labelValue = Label()
    val value = Value<Double>(0.0)

    init {
        labelName.text = name + ": "
        labelValue.text = "0.0"
        labelValue.styleClass.add("label-value")
        children.addAll(labelName, labelValue)

        setOnDragOver { event ->
            if(event.dragboard.hasContent(DragAndDropType.INTERNALOBJECT)){
                event.acceptTransferModes(TransferMode.LINK)
            }
            event.consume()
        }

        setOnDragDropped { event ->
            val dragboard = event.dragboard
            var success = false
            if(dragboard.hasContent(DragAndDropType.INTERNALOBJECT)){
                onInputOutputConnected(Main.internalObject, this)
                val obj = Main.internalObject
                if(obj is Graph){
                    obj.graphWindow.value.input = value
                }
                success = true
            }
            event.isDropCompleted = success
            event.consume()
        }

        value.onChanged { newValue ->
            labelValue.text = newValue.toString()
        }
    }

    var onInputOutputConnected: (Any, Any) -> Unit = { _, _ ->  }

    override fun toString(): String {
        return name
    }
}
