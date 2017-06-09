package spixie

import javafx.scene.control.Label
import javafx.scene.layout.VBox

class Multiplier : VBox(), Element {
    val radius = Value(30.0, 1.0, "Radius")
    val phase = Value(0.0, 0.001, "Phase")
    val size = Value(15.0, 1.0, "Size")
    val count = Value(5.0, 0.1, "Count")

    init {
        children.addAll(Label("Multiplier"))
        children.addAll(radius, phase, size, count)
    }

    override val values: Array<Value.Item>
        get() = arrayOf(radius.item(), phase.item(), size.item(), count.item())

    override fun addGraph(outValue: Value) {
        val graph = Graph(this, outValue)
        children.addAll(graph)
    }
}
