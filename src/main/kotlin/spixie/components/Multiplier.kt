package spixie.components

import javafx.scene.control.ScrollPane
import spixie.Component
import spixie.ComponentObject
import spixie.ComponentProperties
import spixie.RenderBufferBuilder

class Multiplier : Component {
    override val componentProperties: ComponentProperties = ComponentProperties()

    override fun createPropsPane(): MultiplierProps {
        return MultiplierProps()
    }

    override fun getPropsPane(): ScrollPane {
        return componentProperties
    }

    override fun toString(): String {
        return "Multiplier"
    }

    override fun renderObject(componentObject: ComponentObject, renderBufferBuilder: RenderBufferBuilder) {
        val props = componentObject.props
        if(props is MultiplierProps){
            val radius = props.radius.get()
            val phase = props.rotate.get()
            val size = props.size.get()
            val count = props.count.get()
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
    }
}