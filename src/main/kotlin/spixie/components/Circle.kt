package spixie.components

import javafx.scene.layout.VBox
import spixie.*

class Circle(x:Double, y:Double): VisualEditorComponent(x, y), SpixieHashable {
    val radius = ValueControl(30.0, 1.0, "Radius")
    val rotate = ValueControl(0.0, 0.001, "Rotate")
    val size = ValueControl(15.0, 1.0, "Size")
    val count = ValueControl(5.0, 0.1, "Count")

    init {
        val vBox = VBox()
        children.addAll(vBox)
        vBox.children.addAll(radius, rotate, size, count)

        radius.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        rotate.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        size.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        count.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
    }

    fun render(renderBufferBuilder: RenderBufferBuilder){
        val radius = radius.value.value
        val phase = rotate.value.value
        val size = size.value.value
        val count = count.value.value
        var i = 0
        while (i < count) {
            renderBufferBuilder.addParticle(
                    (Math.cos(Math.PI * 2 / count * i + phase * Math.PI * 2.0) * radius).toFloat(),
                    (Math.sin(Math.PI * 2 / count * i + phase * Math.PI * 2.0) * radius).toFloat(),
                    size.toFloat(),
                    0.0f,
                    1.0f,
                    0.0f,
                    1.0f
            )
            i++
        }
    }

    override fun spixieHash(): Long {
        return radius.value.value.raw() mix
                rotate.value.value.raw() mix
                size.value.value.raw() mix
                count.value.value.raw()
    }

    var onValueInputOutputConnected: (Any, Any) -> Unit = { _, _ ->  }
}
