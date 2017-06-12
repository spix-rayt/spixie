package spixie

import javafx.scene.control.ScrollPane

interface Component: ComponentsListItem {
    val componentProperties:ComponentProperties
    fun genPropsPane():ScrollPane
    fun renderObject(componentObject: ComponentObject, particlesBuilder: ParticlesBuilder)
}
