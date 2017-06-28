package spixie.components

import javafx.scene.control.ScrollPane
import spixie.Component
import spixie.ComponentObject
import spixie.ComponentProperties
import spixie.RenderBufferBuilder

class Root: Component {
    override val componentProperties: ComponentProperties = ComponentProperties()
    override fun createPropsPane(): ScrollPane {
        return ScrollPane()
    }

    override fun getPropsPane(): ScrollPane {
        return componentProperties
    }

    override fun toString(): String {
        return "Root"
    }

    override fun renderObject(componentObject: ComponentObject, renderBufferBuilder: RenderBufferBuilder) {

    }
}
