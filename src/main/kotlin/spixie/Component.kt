package spixie

import javafx.scene.control.ScrollPane

interface Component: ComponentsListItem {
    val componentProperties:ComponentProperties
    fun createPropsPane():ScrollPane
    fun renderObject(componentObject: ComponentObject, renderBufferBuilder: RenderBufferBuilder)
}
