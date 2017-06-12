package spixie.components

import javafx.scene.control.ScrollPane
import spixie.Component
import spixie.ComponentObject
import spixie.ComponentProperties
import spixie.ParticlesBuilder

class Root: Component {
    override val componentProperties: ComponentProperties = ComponentProperties()
    override fun genPropsPane(): ScrollPane {
        return ScrollPane()
    }

    override fun getPropsPane(): ScrollPane {
        return componentProperties
    }

    override fun toString(): String {
        return "Root"
    }

    override fun renderObject(componentObject: ComponentObject, particlesBuilder: ParticlesBuilder) {

    }
}