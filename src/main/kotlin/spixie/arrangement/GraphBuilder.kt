package spixie.arrangement

import io.reactivex.Observable
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import spixie.Core
import spixie.Main
import spixie.NumberControl
import spixie.static.linearInterpolate
import spixie.static.rand
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
        val intervalValue = NumberControl(1.0, "Interval").limitMin(1.0)
        val seedValue = NumberControl(1.0, "Seed").limitMin(0.0)

        mode(start, end, listOf(intervalValue, seedValue)){ fragment->
            val seed = seedValue.value.toLong() + 1L
            val interval = intervalValue.value.toInt()
            fragment.data = FloatArray(fragment.data.size){ i->
                val n = i
                val d = n%interval
                val v1 = n-d
                rand(0, 0, 0, 0, seed, (v1+start).toLong())
            }
        }
    }

    private fun sineMode() {
        val startValue = NumberControl(graph.data.getRightValue(start).let { if (it.isNaN()) 0.0 else it.toDouble() }, "Start").limitMin(-1.0).limitMax(1.0)
        val frequencyValue = NumberControl(1.0, "Frequency").limitMin(0.01)

        mode(start, end, listOf(startValue, frequencyValue)){ fragment->
            fragment.data = FloatArray(fragment.data.size){ i->
                val t = ((i / 100.0) *frequencyValue.value + (startValue.value/2+0.75)*4) * Math.PI / 2
                ((Math.sin(t)+1.0)/2).toFloat()
            }
        }
    }

    private fun curveMode(){
        val startValue = NumberControl(graph.data.getRightValue(start).let { if (it.isNaN()) 0.0 else it.toDouble() }, "Start").limitMin(0.0).limitMax(1.0)
        val curvatureValue = NumberControl(0.0, "Curvature")
        val endValue = NumberControl(graph.data.getLeftValue(end).let { if (it.isNaN()) 0.0 else it.toDouble() }, "End").limitMin(0.0).limitMax(1.0)
        mode(start, end, listOf(startValue, curvatureValue, endValue)) { fragment->
            fragment.data = FloatArray(fragment.data.size){ i->
                val curvature = if(startValue.value > endValue.value) -curvatureValue.value else curvatureValue.value
                val t = if(curvature > 0){
                    1.0-(i / (end - start).toDouble())
                }else {
                    (i / (end - start).toDouble())
                }
                val curvaturePower = if(curvature>0) curvature+1.0 else - curvature+1.0

                if(curvature <= 0){
                    linearInterpolate(startValue.value, endValue.value, Math.pow(t, curvaturePower)).toFloat()
                }else{
                    linearInterpolate(endValue.value, startValue.value, Math.pow(t, curvaturePower)).toFloat()
                }
            }
        }
    }

    private fun tanhMode(){
        val startValue = NumberControl(graph.data.getRightValue(start).let { if (it.isNaN()) 0.0 else it.toDouble() }, "Start").limitMin(0.0).limitMax(1.0)
        val stretchValue = NumberControl(10.0, "Stretch").limitMin(0.01)
        val endValue = NumberControl(graph.data.getLeftValue(end).let { if (it.isNaN()) 0.0 else it.toDouble() }, "End").limitMin(0.0).limitMax(1.0)

        mode(start, end, listOf(startValue, stretchValue, endValue)){ fragment->
            val min = min(startValue.value, endValue.value)
            val max = max(startValue.value, endValue.value)
            fragment.data = FloatArray(fragment.data.size){ i->
                val t = (i / (end - start).toDouble()*2-1)*stretchValue.value* sign(endValue.value - startValue.value)
                (((tanh(t)+tanh(stretchValue.value))/(tanh(stretchValue.value)*2)*(max-min))+min).toFloat()
            }
        }
    }

    private inline fun mode(start: Int, end: Int, valueControls: List<NumberControl>, crossinline processData: (fragment: GraphData.Fragment) -> Unit){
        children.clear()
        val fragment = GraphData.Fragment(start, FloatArray(end-start+1))
        graph.data.add(fragment)
        Observable.merge(valueControls.map { it.changes }.plus(Observable.just(Unit))).sample(16, TimeUnit.MILLISECONDS).subscribe {
            processData(fragment)
            Core.arrangementWindow.redrawGraph(graph)
        }
        children.addAll(VBox().apply { children.addAll(valueControls) })
    }
}