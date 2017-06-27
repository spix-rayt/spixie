package spixie.components

import javafx.scene.control.ScrollPane
import spixie.Component
import spixie.ComponentObject
import spixie.ComponentProperties
import spixie.RenderBufferBuilder

class ParticleSpray :Component {
    override fun getPropsPane(): ScrollPane {
        return componentProperties
    }

    override val componentProperties: ComponentProperties = ComponentProperties()

    override fun toString(): String {
        return "ParticleSpray"
    }

    override fun createPropsPane(): ScrollPane {
        return ParticleSprayProps()
    }

    override fun renderObject(componentObject: ComponentObject, renderBufferBuilder: RenderBufferBuilder) {

    }
}