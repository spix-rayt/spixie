package spixie

import io.reactivex.subjects.PublishSubject
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import spixie.static.linearInterpolate
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.tanh

class GraphBuilder(val start:Int, val end:Int, val graph: ArrangementGraph): Pane() {
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
        children.clear()
        val startValue = ValueControl(graph.data.getValue(start).toDouble(), 0.001, "Start").limitMin(-1.0).limitMax(1.0)
        val frequencyValue = ValueControl(1.0, 0.01, "Frequency").limitMin(0.01).limitMax(32.0)
        val update = PublishSubject.create<Unit>()
        update.sample(16, TimeUnit.MILLISECONDS).subscribe {
            graph.data.resizeIfNeed(end+1)
            for(i in start..end){
                var t = (((i - start) / 100.0) *frequencyValue.value.value + (startValue.value.value/2+0.75)*4) * Math.PI / 2
                graph.data.points[i] = ((Math.sin(t)+1.0)/2).toFloat()
            }
            Main.arrangementWindow.updateGraph(graph)
        }
        update.onNext(Unit)
        startValue.value.changes.subscribe { update.onNext(Unit) }
        frequencyValue.value.changes.subscribe { update.onNext(Unit) }
        val vBox = VBox(startValue, frequencyValue)
        children.addAll(vBox)
    }

    private fun curveMode(){
        children.clear()
        val startValue = ValueControl(graph.data.getValue(start).toDouble(), 0.001, "Start").limitMin(0.0).limitMax(1.0)
        val curvatureValue = ValueControl(0.5, 0.001, "Curvature").limitMin(0.0).limitMax(1.0)
        val endValue = ValueControl(graph.data.getValue(end).toDouble(), 0.001, "End").limitMin(0.0).limitMax(1.0)
        val update = PublishSubject.create<Unit>()
        update.sample(16, TimeUnit.MILLISECONDS).subscribe {
            graph.data.resizeIfNeed(end+1)
            for(i in start..end){
                var t = (i - start) / (end - start).toDouble()
                val cy = startValue.value.value + (endValue.value.value - startValue.value.value)*curvatureValue.value.value
                val y1 = linearInterpolate(startValue.value.value, cy, t)
                val y2 = linearInterpolate(cy, endValue.value.value, t)
                graph.data.points[i] = linearInterpolate(y1, y2, t).toFloat()
            }
            Main.arrangementWindow.updateGraph(graph)
        }
        update.onNext(Unit)
        startValue.value.changes.subscribe { update.onNext(Unit) }
        curvatureValue.value.changes.subscribe { update.onNext(Unit) }
        endValue.value.changes.subscribe { update.onNext(Unit) }
        val vBox = VBox(startValue, curvatureValue, endValue)
        children.addAll(vBox)
    }

    private fun tanhMode(){
        children.clear()
        val startValue = ValueControl(graph.data.getValue(start).toDouble(), 0.001, "Start").limitMin(0.0).limitMax(1.0)
        val stretchValue = ValueControl(10.0, 0.01, "Stretch").limitMin(0.01)
        val endValue = ValueControl(graph.data.getValue(end).toDouble(), 0.001, "End").limitMin(0.0).limitMax(1.0)
        val update = PublishSubject.create<Unit>()
        update.sample(16, TimeUnit.MILLISECONDS).subscribe {
            graph.data.resizeIfNeed(end+1)
            val min = min(startValue.value.value, endValue.value.value)
            val max = max(startValue.value.value, endValue.value.value)
            for(i in start..end){
                val t = ((i - start) / (end - start).toDouble()*2-1)*stretchValue.value.value* sign(endValue.value.value - startValue.value.value)
                graph.data.points[i] = (((tanh(t)+tanh(stretchValue.value.value))/(tanh(stretchValue.value.value)*2)*(max-min))+min).toFloat()
            }
            Main.arrangementWindow.updateGraph(graph)
        }
        update.onNext(Unit)
        startValue.value.changes.subscribe { update.onNext(Unit) }
        stretchValue.value.changes.subscribe { update.onNext(Unit) }
        endValue.value.changes.subscribe { update.onNext(Unit) }

        children.addAll(VBox(startValue,stretchValue,endValue))
    }
}