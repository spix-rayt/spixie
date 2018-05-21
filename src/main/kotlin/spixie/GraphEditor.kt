package spixie

import io.reactivex.Observable
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
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
        val startHeightValue = ValueControl(1.0, 0.01, "StartHeight").limitMin(0.0).limitMax(1.0)
        val startShiftValue = ValueControl(0.0, 0.01, "StartShift").limitMin(0.0).limitMax(1.0)
        val endHeightValue = ValueControl(1.0, 0.01, "EndHeight").limitMin(0.0).limitMax(1.0)
        val endShiftValue = ValueControl(0.0, 0.01, "EndShift").limitMin(0.0).limitMax(1.0)

        mode(start, end, listOf(startHeightValue, startShiftValue, endHeightValue, endShiftValue)){ data ->
            val startHeight = startHeightValue.value
            val startShift = startShiftValue.value
            val endHeight = endHeightValue.value
            val endShift = endShiftValue.value
            for(i in 0..(end-start)){
                val t = i / (end-start).toDouble()
                val height = linearInterpolate(startHeight, endHeight, t)
                val shift = linearInterpolate(startShift, endShift, t)
                if(data.points[i] == GraphData.JUMP_POINT){
                    data.jumpPoints[i] = (data.jumpPoints[i]!!.first*height + (1.0 - height)*shift).toFloat() to (data.jumpPoints[i]!!.second*height + (1.0 - height)*shift).toFloat()
                }else{
                    data.points[i] = (data.points[i]*height + (1.0 - height)*shift).toFloat()
                }
            }
        }
    }

    private inline fun mode(start: Int, end: Int, valueControls: List<ValueControl>, crossinline processData: (copy: GraphData) -> Unit){
        children.clear()
        val copy = graph.data.copy(start, end)
        Observable.merge(valueControls.map { it.changes }.plus(Observable.just(Unit))).sample(16, TimeUnit.MILLISECONDS).subscribe {
            graph.data.resizeIfNeed(end+1)
            val data = GraphData().apply { points = copy.points.clone(); jumpPoints.putAll(copy.jumpPoints) }
            processData(data)
            graph.data.paste(start,  data)
            Main.arrangementWindow.redrawGraph(graph)
        }
        children.addAll(VBox().apply { children.addAll(valueControls) })
    }
}