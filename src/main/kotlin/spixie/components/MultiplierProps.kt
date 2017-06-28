package spixie.components

import javafx.scene.control.ScrollPane
import javafx.scene.layout.VBox
import spixie.Value

class MultiplierProps: ScrollPane() {
    val radius = Value(30.0, 1.0, "Radius", true)
    val rotate = Value(0.0, 0.001, "Rotate", true)
    val size = Value(15.0, 1.0, "Size", true)
    val count = Value(5.0, 0.1, "Count", true)

    init {
        val vBox = VBox()
        content = vBox
        vBox.children.addAll(radius, rotate, size, count)
    }
}
