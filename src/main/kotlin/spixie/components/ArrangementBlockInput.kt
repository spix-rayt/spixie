package spixie.components

import javafx.scene.layout.VBox
import spixie.SpixieHashable
import spixie.ValueLabel
import spixie.VisualEditorComponent

class ArrangementBlockInput (x:Double, y:Double) : VisualEditorComponent(x, y), SpixieHashable {
    val time = ValueLabel("Block time")
    init {
        val vBox = VBox()
        children.addAll(vBox)
        vBox.children.addAll(time)

        time.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
    }

    var onValueInputOutputConnected: (Any, Any) -> Unit = { _, _ ->  }
}