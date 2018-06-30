package spixie.arrangement

import io.reactivex.Observable
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import spixie.Main
import spixie.NumberControl
import spixie.static.linearInterpolate
import spixie.visualEditor.GraphData
import java.util.concurrent.TimeUnit

class GraphEditor(private val start:Int, private val end:Int, private val graph: ArrangementGraph): Pane() {
    init {
        assert(end > start)

        style = "-fx-border-color: #9A12B3FF; -fx-border-width: 1; -fx-background-color: #FFFFFFFF;"
        width = 500.0
        height = 300.0
        val hBox = HBox(2.0)
        hBox.children.addAll(
                Button("Border").apply { setOnAction { borderMode() } }
        )
        children.addAll(hBox)

        setOnMousePressed {
            it.consume()
        }
    }

    private fun borderMode() {
        children.clear()
        val startHeightValue = NumberControl(1.0, 0.01, "StartHeight").limitMin(0.0).limitMax(1.0)
        val startShiftValue = NumberControl(0.0, 0.01, "StartShift").limitMin(0.0).limitMax(1.0)
        val endHeightValue = NumberControl(1.0, 0.01, "EndHeight").limitMin(0.0).limitMax(1.0)
        val endShiftValue = NumberControl(0.0, 0.01, "EndShift").limitMin(0.0).limitMax(1.0)

        mode(start, end, listOf(startHeightValue, startShiftValue, endHeightValue, endShiftValue)){ data ->
            val startHeight = startHeightValue.value
            val startShift = startShiftValue.value
            val endHeight = endHeightValue.value
            val endShift = endShiftValue.value
            data.forEach { fragment->
                for(i in 0..fragment.data.lastIndex){
                    val t = (i+fragment.start-start) / (end-start).toDouble()
                    val height = linearInterpolate(startHeight, endHeight, t)
                    val shift = linearInterpolate(startShift, endShift, t)
                    fragment.data[i] = (fragment.data[i]*height + (1.0 - height)*shift).toFloat()
                }
            }
        }
    }

    private inline fun mode(start: Int, end: Int, valueControls: List<NumberControl>, crossinline processData: (copy: List<GraphData.Fragment>) -> Unit){
        children.clear()
        val copy = graph.data.copy(start, end)
        Observable.merge(valueControls.map { it.changes }.plus(Observable.just(Unit))).sample(16, TimeUnit.MILLISECONDS).subscribe {
            val data = copy.map { GraphData.Fragment(it.start, it.data.clone()) }
            processData(data)
            graph.data.delete(start, end)
            data.forEach {
                graph.data.add(it)
            }
            Main.arrangementWindow.redrawGraph(graph)
        }
        children.addAll(VBox().apply { children.addAll(valueControls) })
    }
}