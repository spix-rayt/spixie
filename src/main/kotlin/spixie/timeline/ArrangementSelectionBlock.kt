package spixie.timeline

import io.reactivex.subjects.BehaviorSubject
import javafx.scene.layout.Region
import org.apache.commons.lang3.math.Fraction
import spixie.static.F_100
import spixie.timelineWindow
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

    var beatStart: Fraction = Fraction.ZERO
        set(value) {
            field = value
            updateLayout()
            timelineWindow.graphBuilderGroup.children.clear()
        }
    var beatEnd: Fraction = Fraction.ZERO
        set(value) {
            field = value
            updateLayout()
            timelineWindow.graphBuilderGroup.children.clear()
        }

    var graph: ArrangementGraph? = null

    init {
        zoom.subscribe {
            updateLayout()
        }
        strictHeight = 100.0
    }

    private fun updateLayout() {
        strictWidth = Fraction.getFraction(100, 64).multiplyBy(beatEnd.subtract(beatStart)).multiplyBy(zoom.value!!).toDouble()
        layoutX = Fraction.getFraction(100, 64).multiplyBy(beatStart).multiplyBy(zoom.value!!).toDouble()
    }

    fun buildGraph() {
        graph?.let { graph->
            val start = beatStart.multiplyBy(F_100).toInt()
            val end = beatEnd.multiplyBy(F_100).toInt()
            val graphBuilder = GraphBuilder(start, if (end > start) end else start + 1, graph)
            val subscribe = zoom.subscribe {
                graphBuilder.layoutX = layoutX
                graphBuilder.layoutY = layoutY + height + 10.0
            }
            graphBuilder.parentProperty().addListener { _, _, newValue -> if(newValue == null) subscribe.dispose() }
            timelineWindow.graphBuilderGroup.children.setAll(graphBuilder)
        }
    }

    fun editGraph() {
        graph?.let { graph->
            val start = beatStart.multiplyBy(F_100).toInt()
            val end = beatEnd.multiplyBy(F_100).toInt()
            val graphEditor = GraphEditor(start, if (end > start) end else start + 1, graph)
            val subscribe = zoom.subscribe {
                graphEditor.layoutX = layoutX
                graphEditor.layoutY = layoutY + height + 10.0
            }
            graphEditor.parentProperty().addListener { _, _, newValue -> if(newValue == null) subscribe.dispose() }
            timelineWindow.graphBuilderGroup.children.setAll(graphEditor)
        }
    }

    private var copyData = listOf<GraphData.Fragment>()
    private var copyStart = 0

    fun copy() {
        graph?.let { graph ->
            copyData = graph.data.copy(beatStart.multiplyBy(F_100).toInt(), beatEnd.multiplyBy(F_100).toInt())
            copyStart = beatStart.multiplyBy(F_100).toInt()
        }
    }

    fun paste() {
        graph?.let { graph->
            val offset = beatStart.multiplyBy(F_100).toInt() - copyStart
            copyData.forEach {
                graph.data.add(GraphData.Fragment(it.start+offset, it.data.clone()))
            }
            timelineWindow.needRedrawAllGraphs = true
        }
    }

    fun del() {
        graph?.let { graph ->
            graph.data.delete(beatStart.multiplyBy(F_100).toInt(), beatEnd.multiplyBy(F_100).toInt())
            timelineWindow.needRedrawAllGraphs = true
        }
    }

    fun duplicate() {
        graph?.let { graph->
            val p = graph.data.copy(beatStart.multiplyBy(F_100).toInt(), beatEnd.multiplyBy(F_100).toInt())
            val pStart = beatStart.multiplyBy(F_100).toInt()
            val l = beatEnd.subtract(beatStart)
            beatEnd = beatEnd.add(l)
            beatStart = beatStart.add(l)
            val offset = beatStart.multiplyBy(F_100).toInt() - pStart
            p.forEach {
                graph.data.add(GraphData.Fragment(it.start+offset, it.data.clone()))
            }
            timelineWindow.needRedrawAllGraphs = true
        }
    }

    fun reverse() {
        graph?.let { graph->
            graph.data.reverse(beatStart.multiplyBy(F_100).toInt(), beatEnd.multiplyBy(F_100).toInt())
            timelineWindow.needRedrawAllGraphs = true
        }
    }
}