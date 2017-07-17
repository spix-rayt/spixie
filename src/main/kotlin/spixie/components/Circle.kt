package spixie.components

import javafx.scene.layout.VBox
import spixie.*

class Circle(x:Double, y:Double): VisualEditorComponent(x, y), SpixieHashable {
    val radius = Value(30.0, 1.0, "Radius", true)
    val rotate = Value(0.0, 0.001, "Rotate", true)
    val size = Value(15.0, 1.0, "Size", true)
    val count = Value(5.0, 0.1, "Count", true)

    init {
        val vBox = VBox()
    children.addAll(vBox)
    vBox.children.addAll(radius, rotate, size, count)
}

    fun render(renderBufferBuilder: RenderBufferBuilder){
        val radius = radius.get()
        val phase = rotate.get()
        val size = size.get()
        val count = count.get()
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
        return radius.get().raw() mix
                rotate.get().raw() mix
                size.get().raw() mix
                count.get().raw()
    }
}
