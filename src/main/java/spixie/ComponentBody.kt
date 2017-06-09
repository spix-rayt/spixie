package spixie

import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox

class ComponentBody : ScrollPane() {
    internal val elements = VBox()

    init {
        val borderPane = BorderPane()
        val addElementButton = Button("Add element")
        borderPane.bottom = addElementButton
        borderPane.center = elements
        content = borderPane

        widthProperty().addListener { _, _, t1 -> addElementButton.prefWidth = t1.toDouble() - 2 }

        addElementButton.onMouseClicked = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                elements.children.addAll(Multiplier())
            }
        }
    }
}
