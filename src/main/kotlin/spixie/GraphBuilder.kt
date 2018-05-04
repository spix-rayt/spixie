package spixie

import io.reactivex.subjects.PublishSubject
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import spixie.static.linearInterpolate
import java.util.concurrent.TimeUnit

class GraphBuilder(val start:Int, val end:Int, val graph: ArrangementGraph): Pane() {
    init {
        width = 500.0
        height = 300.0
        val hBox = HBox(2.0)
        val curveModeButton = Button("Curve")
        curveModeButton.setOnAction { curveMode() }
        val sineModeButton = Button("Sine")
        sineModeButton.setOnAction { sineMode() }
        val randomModeButton = Button("Random")
        randomModeButton.setOnAction { randomMode() }
        hBox.children.addAll(curveModeButton, sineModeButton, randomModeButton)
        children.addAll(hBox)
    }

    private fun randomMode() {
        children.clear()
    }

    private fun sineMode() {
        children.clear()
        val phaseValue = ValueControl(0.0, 0.001, "Phase").limitMax(1.0)
        val frequencyValue = ValueControl(1.0, 0.01, "Frequency").limitMin(0.01).limitMax(32.0)
        val update = PublishSubject.create<Unit>()
        update.sample(16, TimeUnit.MILLISECONDS).subscribe {
            graph.data.del(start, end)
            for(i in start..end){
                var t = (((i - start) / 100.0) *frequencyValue.value.value + phaseValue.value.value*4) * Math.PI / 2
                graph.data.insertOrUpdatePoint(i, (Math.sin(t)+1.0)/2, false)
            }
            Main.workingWindow.arrangementWindow.updateGraph(graph)
        }
        update.onNext(Unit)
        phaseValue.value.changes.subscribe { update.onNext(Unit) }
        frequencyValue.value.changes.subscribe { update.onNext(Unit) }
        val vBox = VBox(phaseValue, frequencyValue)
        children.addAll(vBox)
    }

    private fun curveMode(){
        children.clear()
        val startValue = ValueControl(0.0, 0.001, "Start").limitMax(1.0)
        val curvatureValue = ValueControl(0.5, 0.001, "Curvature").limitMax(1.0)
        val endValue = ValueControl(1.0, 0.001, "End").limitMax(1.0)
        val update = PublishSubject.create<Unit>()
        update.sample(16, TimeUnit.MILLISECONDS).subscribe {
            graph.data.del(start, end)
            for(i in start..end){
                var t = (i - start) / (end - start).toDouble()
                val cy = startValue.value.value + (endValue.value.value - startValue.value.value)*curvatureValue.value.value
                val y1 = linearInterpolate(startValue.value.value, cy, t)
                val y2 = linearInterpolate(cy, endValue.value.value, t)
                graph.data.insertOrUpdatePoint(i, 1.0 - linearInterpolate(y1, y2, t), false)
            }
            Main.workingWindow.arrangementWindow.updateGraph(graph)
        }
        update.onNext(Unit)
        startValue.value.changes.subscribe { update.onNext(Unit) }
        curvatureValue.value.changes.subscribe { update.onNext(Unit) }
        endValue.value.changes.subscribe { update.onNext(Unit) }
        val vBox = VBox(startValue, curvatureValue, endValue)
        children.addAll(vBox)
    }
}