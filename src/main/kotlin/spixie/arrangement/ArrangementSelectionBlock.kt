package spixie.arrangement

import io.reactivex.subjects.BehaviorSubject
import javafx.scene.layout.Region
import org.apache.commons.lang3.math.Fraction
import spixie.Core
import spixie.Main
import spixie.static.F_100
import spixie.visualEditor.GraphData

class ArrangementSelectionBlock(private val zoom:BehaviorSubject<Fraction>): Region() {
    private var strictWidth:Double
        get() = width
        set(value) {
            minWidth = value
            maxWidth = value
        }

    private var strictHeight:Double
        get() = height
        set(value) {
            minHeight = value
            maxHeight = value
        }

    var timeStart: Fraction = Fraction.ZERO
        set(value) {
            field = value
            updateLayout()
            Core.arrangementWindow.graphBuilderGroup.children.clear()
        }
    var timeEnd: Fraction = Fraction.ZERO
        set(value) {
            field = value
            updateLayout()
            Core.arrangementWindow.graphBuilderGroup.children.clear()
        }

    var line = 0
        set(value) {
            field = value
            layoutY = value*100.0
            Core.arrangementWindow.graphBuilderGroup.children.clear()
        }

    var graph: ArrangementGraph? = null

    init {
        zoom.subscribe {
            updateLayout()
        }
        strictHeight = 100.0
    }

    private fun updateLayout(){
        strictWidth = Fraction.getFraction(100, 64).multiplyBy(timeEnd.subtract(timeStart)).multiplyBy(zoom.value!!).toDouble()
        layoutX = Fraction.getFraction(100, 64).multiplyBy(timeStart).multiplyBy(zoom.value!!).toDouble()
    }

    fun buildGraph(){
        graph?.let { graph->
            val start = timeStart.multiplyBy(F_100).toInt()
            val end = timeEnd.multiplyBy(F_100).toInt()
            val graphBuilder = GraphBuilder(start, if (end > start) end else start + 1, graph)
            val subscribe = zoom.subscribe {
                graphBuilder.layoutX = layoutX
                graphBuilder.layoutY = layoutY + height + 10.0
            }
            graphBuilder.parentProperty().addListener { _, _, newValue -> if(newValue == null) subscribe.dispose() }
            Core.arrangementWindow.graphBuilderGroup.children.setAll(graphBuilder)
        }
    }

    fun editGraph(){
        graph?.let { graph->
            val start = timeStart.multiplyBy(F_100).toInt()
            val end = timeEnd.multiplyBy(F_100).toInt()
            val graphEditor = GraphEditor(start, if (end > start) end else start + 1, graph)
            val subscribe = zoom.subscribe {
                graphEditor.layoutX = layoutX
                graphEditor.layoutY = layoutY + height + 10.0
            }
            graphEditor.parentProperty().addListener { _, _, newValue -> if(newValue == null) subscribe.dispose() }
            Core.arrangementWindow.graphBuilderGroup.children.setAll(graphEditor)
        }
    }

    private var copyData = listOf<GraphData.Fragment>()
    private var copyStart = 0

    fun copy(){
        graph?.let { graph ->
            copyData = graph.data.copy(timeStart.multiplyBy(F_100).toInt(), timeEnd.multiplyBy(F_100).toInt())
            copyStart = timeStart.multiplyBy(F_100).toInt()
        }
    }

    fun paste(){
        graph?.let { graph->
            val offset = timeStart.multiplyBy(F_100).toInt() - copyStart
            copyData.forEach {
                graph.data.add(GraphData.Fragment(it.start+offset, it.data.clone()))
            }
            Core.arrangementWindow.needRedrawAllGraphs = true
        }
    }

    fun del() {
        graph?.let { graph ->
            graph.data.delete(timeStart.multiplyBy(F_100).toInt(), timeEnd.multiplyBy(F_100).toInt())
            Core.arrangementWindow.needRedrawAllGraphs = true
        }
    }

    fun duplicate(){
        graph?.let { graph->
            val p = graph.data.copy(timeStart.multiplyBy(F_100).toInt(), timeEnd.multiplyBy(F_100).toInt())
            val pStart = timeStart.multiplyBy(F_100).toInt()
            val l = timeEnd.subtract(timeStart)
            timeEnd = timeEnd.add(l)
            timeStart = timeStart.add(l)
            val offset = timeStart.multiplyBy(F_100).toInt() - pStart
            p.forEach {
                graph.data.add(GraphData.Fragment(it.start+offset, it.data.clone()))
            }
            Core.arrangementWindow.needRedrawAllGraphs = true
        }
    }

    fun reverse(){
        graph?.let { graph->
            graph.data.reverse(timeStart.multiplyBy(F_100).toInt(), timeEnd.multiplyBy(F_100).toInt())
            Core.arrangementWindow.needRedrawAllGraphs = true
        }
    }
}