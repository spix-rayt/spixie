package spixie

import javafx.scene.control.ScrollPane

class ComponentObject(val component: Component, val props:ScrollPane): ComponentsListItem {
    override fun getPropsPane(): ScrollPane {
        return props;
    }

    override fun toString(): String {
        return component.toString()
    }

    fun render(renderBufferBuilder: RenderBufferBuilder){
        component.renderObject(this, renderBufferBuilder)
    }
}
