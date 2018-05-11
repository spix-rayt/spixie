package spixie

import io.reactivex.Observable
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import spixie.static.linearInterpolate
import spixie.visualEditor.GraphData
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.tanh

class GraphBuilder(private val start:Int, private val end:Int, private val graph: ArrangementGraph): Pane() {
    init {
        assert(end > start)

        style = "-fx-border-color: #9A12B3FF; -fx-border-width: 1; -fx-background-color: #FFFFFFFF;"
        width = 500.0
        height = 300.0
        val hBox = HBox(2.0)
        hBox.children.addAll(
                Button("Curve").apply { setOnAction { curveMode() } },
                Button("Sine").apply { setOnAction { sineMode() } },
                Button("TanhMode").apply { setOnAction { tanhMode() } },
                Button("Random").apply { setOnAction { randomMode() } }
        )
        children.addAll(hBox)

        setOnMousePressed {
            it.consume()
        }
    }

    private fun randomMode() {
        children.clear()
    }

    private fun sineMode() {
        val startValue = ValueControl(graph.data.getRightValue(start).toDouble(), 0.001, "Start").limitMin(-1.0).limitMax(1.0)
        val frequencyValue = ValueControl(1.0, 0.01, "Frequency").limitMin(0.01).limitMax(32.0)

        mode(start, end, listOf(startValue, frequencyValue)){
            for(i in start..end){
                val t = (((i - start) / 100.0) *frequencyValue.value + (startValue.value/2+0.75)*4) * Math.PI / 2
                graph.data.points[i] = ((Math.sin(t)+1.0)/2).toFloat()
            }
        }
    }

    private fun curveMode(){
        val startValue = ValueControl(graph.data.getRightValue(start).toDouble(), 0.001, "Start").limitMin(0.0).limitMax(1.0)
        val curvatureValue = ValueControl(0.5, 0.001, "Curvature").limitMin(0.0).limitMax(1.0)
        val endValue = ValueControl(graph.data.getLeftValue(end).toDouble(), 0.001, "End").limitMin(0.0).limitMax(1.0)
        mode(start, end, listOf(startValue, curvatureValue, endValue)) {
            for(i in start..end){
                val t = (i - start) / (end - start).toDouble()
                val cy = startValue.value + (endValue.value - startValue.value)*curvatureValue.value
                val y1 = linearInterpolate(startValue.value, cy, t)
                val y2 = linearInterpolate(cy, endValue.value, t)
                graph.data.points[i] = linearInterpolate(y1, y2, t).toFloat()
            }
        }
    }

    private fun tanhMode(){
        val startValue = ValueControl(graph.data.getRightValue(start).toDouble(), 0.001, "Start").limitMin(0.0).limitMax(1.0)
        val stretchValue = ValueControl(10.0, 0.01, "Stretch").limitMin(0.01)
        val endValue = ValueControl(graph.data.getLeftValue(end).toDouble(), 0.001, "End").limitMin(0.0).limitMax(1.0)

        mode(start, end, listOf(startValue, stretchValue, endValue)){
            val min = min(startValue.value, endValue.value)
            val max = max(startValue.value, endValue.value)
            for(i in start..end){
                val t = ((i - start) / (end - start).toDouble()*2-1)*stretchValue.value* sign(endValue.value - startValue.value)
                graph.data.points[i] = (((tanh(t)+tanh(stretchValue.value))/(tanh(stretchValue.value)*2)*(max-min))+min).toFloat()
            }
        }
    }

    private inline fun mode(start: Int, end: Int, valueControls: List<ValueControl>, crossinline updateData: () -> Unit){
        children.clear()
        val startLeftValue = graph.data.getLeftValue(start).toDouble()
        val endRightValue = graph.data.getRightValue(end).toDouble()
        Observable.merge(valueControls.map { it.changes }.plus(Observable.just(Unit))).sample(16, TimeUnit.MILLISECONDS).subscribe {
            graph.data.resizeIfNeed(end+1)
            updateData()
            graph.data.setJumpPoint(start, startLeftValue.toFloat() to graph.data.points[start])
            graph.data.points[start] = GraphData.JUMP_POINT
            graph.data.setJumpPoint(end, graph.data.points[end] to endRightValue.toFloat())
            graph.data.points[end] = GraphData.JUMP_POINT
            Main.arrangementWindow.updateGraph(graph)
        }
        children.addAll(VBox().apply { children.addAll(valueControls) })
    }
}