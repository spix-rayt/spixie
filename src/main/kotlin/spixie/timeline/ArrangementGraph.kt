package spixie.timeline

import javafx.scene.canvas.Canvas
import spixie.NumberControl
import spixie.renderManager
import spixie.visualEditor.GraphData

class ArrangementGraph {
    val data = GraphData()

    @Transient
    val canvas = Canvas(1.0, 100.0)

    val rangeMinControl = NumberControl(0.0, "Min")

    val rangeMaxControl = NumberControl(1.0, "Max")

    init {
        rangeMinControl.changes.subscribe {
            renderManager.requestRender()
        }
        rangeMaxControl.changes.subscribe {
            renderManager.requestRender()
        }
    }
}