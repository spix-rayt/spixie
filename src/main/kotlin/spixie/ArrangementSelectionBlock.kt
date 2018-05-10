package spixie

import io.reactivex.subjects.BehaviorSubject
import javafx.scene.layout.Region
import org.apache.commons.math3.fraction.Fraction

class ArrangementSelectionBlock(val zoom:BehaviorSubject<Fraction>): Region() {
    var strictWidth:Double
        get() = width
        set(value) {
            minWidth = value
            maxWidth = value
        }

    var strictHeight:Double
        get() = height
        set(value) {
            minHeight = value
            maxHeight = value
        }

    var timeStart = Fraction(0)
        set(value) {
            field = value
            updateLayout()
            Main.arrangementWindow.graphBuilderGroup.children.clear()
        }
    var timeEnd = Fraction(0)
        set(value) {
            field = value
            updateLayout()
            Main.arrangementWindow.graphBuilderGroup.children.clear()
        }

    var line = 0
        set(value) {
            field = value
            layoutY = value*100.0
            Main.arrangementWindow.graphBuilderGroup.children.clear()
        }

    init {
        zoom.subscribe {
            updateLayout()
        }
        strictHeight = 100.0
    }

    private fun updateLayout(){
        strictWidth = Fraction(100, 64).multiply(timeEnd.subtract(timeStart)).multiply(zoom.value!!).toDouble()
        layoutX = Fraction(100, 64).multiply(timeStart).multiply(zoom.value!!).toDouble()
    }

    private fun newGraph(): ArrangementGraph {
        val newGraph = ArrangementGraph()
        Main.arrangementWindow.graphCanvases.children.addAll(newGraph.canvas)
        newGraph.canvas.width = Main.arrangementWindow.width + 200.0
        newGraph.canvas.layoutY = line*100.0
        return newGraph
    }

    fun buildGraph(){
        val graph = Main.arrangementWindow.graphs.getOrPut(line) { newGraph() }
        val start = timeStart.multiply(100).toInt()
        val end = timeEnd.multiply(100).toInt()
        val graphBuilder = GraphBuilder(start, if(end>start) end else start+1, graph)
        val subscribe = zoom.subscribe {
            graphBuilder.layoutX = layoutX
            graphBuilder.layoutY = layoutY + height + 10.0
        }
        graphBuilder.parentProperty().addListener { _, _, newValue -> if(newValue != null) subscribe.dispose() }
        Main.arrangementWindow.graphBuilderGroup.children.addAll(graphBuilder)
    }

    var copyData = floatArrayOf() to mapOf<Int, Pair<Float, Float>>()
    var copyLength = 0

    fun copy(){
        Main.arrangementWindow.graphs[line]?.let { graph ->
            copyData = graph.data.copy(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
            copyLength = timeEnd.multiply(100).toInt() - timeStart.multiply(100).toInt()
        }
    }

    fun paste(){
        val graph = Main.arrangementWindow.graphs.getOrPut(line) { newGraph() }
        graph.data.del(timeStart.multiply(100).toInt(), timeStart.multiply(100).toInt() + copyLength)
        graph.data.paste(timeStart.multiply(100).toInt(), copyData)
        Main.arrangementWindow.needUpdateAllGraphs = true
    }

    fun del() {
        Main.arrangementWindow.graphs[line]?.let { graph ->
            graph.data.del(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
            Main.arrangementWindow.needUpdateAllGraphs = true
        }
    }

    fun duplicate(){
        val graph = Main.arrangementWindow.graphs.getOrPut(line) { newGraph() }
        val p = graph.data.copy(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
        val l = timeEnd.subtract(timeStart)
        timeEnd = timeEnd.add(l)
        timeStart = timeStart.add(l)
        graph.data.del(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
        graph.data.paste(timeStart.multiply(100).toInt(), p)
        Main.arrangementWindow.needUpdateAllGraphs = true
    }

    fun reverse(){
        val graph = Main.arrangementWindow.graphs.getOrPut(line) { newGraph() }
        graph.data.reverse(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
        Main.arrangementWindow.needUpdateAllGraphs = true
    }
}