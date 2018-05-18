package spixie.visualEditor

import spixie.ArrangementGraphsContainer

class ComponentListGraphItem(val graph: ArrangementGraphsContainer) {
    override fun toString(): String {
        return "Graph: ${graph.name.value}"
    }
}