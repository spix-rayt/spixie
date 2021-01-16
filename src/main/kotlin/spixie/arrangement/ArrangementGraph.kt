package spixie.arrangement

import javafx.scene.canvas.Canvas
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import spixie.Core
import spixie.NumberControl
import spixie.visualEditor.GraphData

class ArrangementGraph {
    val data = GraphData()

    @Transient
    val canvas = Canvas(1.0, 100.0)

    val rangeMinControl = NumberControl(0.0, "Min")

    val rangeMaxControl = NumberControl(1.0, "Max")

    init {
        rangeMinControl.changes.subscribe {
            GlobalScope.launch {
                Core.renderManager.requestRender()
            }
        }
        rangeMaxControl.changes.subscribe {
            GlobalScope.launch {
                Core.renderManager.requestRender()
            }
        }
    }
}