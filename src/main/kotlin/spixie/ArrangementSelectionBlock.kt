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
        }
    var timeEnd = Fraction(0)
        set(value) {
            field = value
            updateZoom()
        }

    var line = 0
        set(value) {
            field = value
            layoutY = value*100.0
        }

    init {
        updateZoom()
        strictHeight = 100.0
    }

    fun updateZoom(){
        strictWidth = Fraction(100, 64).multiply(timeEnd.subtract(timeStart)).multiply(zoom.value).toDouble()
        layoutX = Fraction(100, 64).multiply(timeStart).multiply(zoom.value).toDouble()
    }

    fun buildGraph(){
        val arrangementWindow = Main.workingWindow.arrangementWindow
        val graph = arrangementWindow.graphs.getOrPut(line) {
            val newGraph = ArrangementGraph()
            Main.workingWindow.arrangementWindow.graphCanvases.children.addAll(newGraph.canvas)
            newGraph.canvas.width = arrangementWindow.width + 200.0
            newGraph.canvas.layoutY = line*100.0
            newGraph
        }
        val c = (timeEnd.subtract(timeStart).multiply(100)).toInt()
        graph.data.insertOrUpdatePoint(timeStart.toDouble(), graph.data.getValue(timeStart.toDouble()), false)
        graph.data.insertOrUpdatePoint(timeEnd.toDouble(), graph.data.getValue(timeEnd.toDouble()), false)
        for(i in 1 until c){
            graph.data.insertOrUpdatePoint(timeStart.toDouble() + i * 0.01, (Math.sin(i.toDouble()*0.1) + 1.0) / 2, false)
        }
        arrangementWindow.updateGraphs.run()
    }
}