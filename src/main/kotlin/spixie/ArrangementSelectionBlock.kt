package spixie

import javafx.scene.layout.Region
import org.apache.commons.math3.fraction.Fraction

class ArrangementSelectionBlock(val zoom:FractionImmutablePointer): Region() {
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
            updateZoom()
            Main.workingWindow.arrangementWindow.graphBuilderGroup.children.clear()
        }
    var timeEnd = Fraction(0)
        set(value) {
            field = value
            updateZoom()
            Main.workingWindow.arrangementWindow.graphBuilderGroup.children.clear()
        }

    var line = 0
        set(value) {
            field = value
            layoutY = value*100.0
            Main.workingWindow.arrangementWindow.graphBuilderGroup.children.clear()
        }

    init {
        updateZoom()
        strictHeight = 100.0
    }

    fun updateZoom(){
        strictWidth = Fraction(100, 64).multiply(timeEnd.subtract(timeStart)).multiply(zoom.value).toDouble()
        layoutX = Fraction(100, 64).multiply(timeStart).multiply(zoom.value).toDouble()
    }

    private fun newGraph(): ArrangementGraph {
        val newGraph = ArrangementGraph()
        Main.workingWindow.arrangementWindow.graphCanvases.children.addAll(newGraph.canvas)
        newGraph.canvas.width = Main.workingWindow.arrangementWindow.width + 200.0
        newGraph.canvas.layoutY = line*100.0
        return newGraph
    }

    fun buildGraph(){
        val graph = Main.workingWindow.arrangementWindow.graphs.getOrPut(line) { newGraph() }
        val graphBuilder = GraphBuilder(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt(), graph)
        graphBuilder.layoutX = layoutX
        graphBuilder.layoutY = layoutY + height+10.0
        Main.workingWindow.arrangementWindow.graphBuilderGroup.children.addAll(graphBuilder)
    }

    var pointsCopy = ArrayList<Point>()
    var copyLength = 0

    fun copy(){
        Main.workingWindow.arrangementWindow.graphs[line]?.let { graph ->
            pointsCopy = graph.data.copy(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
            copyLength = timeEnd.multiply(100).toInt() - timeStart.multiply(100).toInt()
        }
    }

    fun paste(){
        val graph = Main.workingWindow.arrangementWindow.graphs.getOrPut(line) { newGraph() }
        graph.data.del(timeStart.multiply(100).toInt(), timeStart.multiply(100).toInt() + copyLength)
        graph.data.paste(timeStart.multiply(100).toInt(), pointsCopy)
        Main.workingWindow.arrangementWindow.updateGraphs.run()
    }

    fun del() {
        Main.workingWindow.arrangementWindow.graphs[line]?.let { graph ->
            graph.data.del(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
            Main.workingWindow.arrangementWindow.updateGraphs.run()
        }
    }

    fun duplicate(){
        val graph = Main.workingWindow.arrangementWindow.graphs.getOrPut(line) { newGraph() }
        val p = graph.data.copy(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
        val l = timeEnd.subtract(timeStart)
        timeEnd = timeEnd.add(l)
        timeStart = timeStart.add(l)
        graph.data.del(timeStart.multiply(100).toInt(), timeEnd.multiply(100).toInt())
        graph.data.paste(timeStart.multiply(100).toInt(), p)
        Main.workingWindow.arrangementWindow.updateGraphs.run()
    }
}