package spixie

import javafx.scene.layout.Region

open class VisualEditorComponent(x:Double, y:Double):Region(), SpixieHashable {
    init {
        style = "-fx-border-color: #9A12B3FF; -fx-border-width: 1;"
        minWidth = 100.0
        minHeight= 40.0

        layoutX = x
        layoutY = y
    }

    override fun spixieHash(): Long {
        return magic.toLong()
    }
}