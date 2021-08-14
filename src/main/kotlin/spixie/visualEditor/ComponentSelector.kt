package spixie.visualEditor


import io.reactivex.rxjavafx.observables.JavaFxObservable
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import spixie.static.pickFirstConnectableByType
import spixie.timeline.ArrangementGraphsContainer
import spixie.timelineWindow
import spixie.visualEditor.components.*
import spixie.visualEditor.pins.ComponentPin

class ComponentSelector(
    x: Double,
    y:Double,
    private val containerChildrens: ObservableList<Node>,
    private val inputFilter: Class<out ComponentPin>? = null,
    private val outputFilter: Class<out ComponentPin>? = null,
    private val result: (component: Component) -> Unit
): BorderPane() {
    private val listView = ListView<Any>().apply {
        setOnMouseClicked {
            createSelected()
        }
    }

    private val scrollPane = ScrollPane(listView)

    private val textField = TextField()

    init {
        textField.focusedProperty().addListener { _, _, newValue ->
            if(!newValue) {
                scene?.let { scene ->
                    if(scene.focusOwner == scrollPane || scene.focusOwner == listView) {
                        textField.requestFocus()
                    } else {
                        containerChildrens.remove(this@ComponentSelector)
                    }
                }
            }
        }

        JavaFxObservable.valuesOf(textField.textProperty()).startWith(textField.text).subscribe { filterText->
            val filterRegex = Regex(filterText.lowercase().map { ".*$it" }.joinToString(separator = "") + ".*")
            val selectedItem = listView.selectionModel.selectedItem
            listView.items.clear()
            listView.items.addAll(
                *basicComponentTemplates
                    .filter { component -> inputFilter == null || component.inputPins.pickFirstConnectableByType(inputFilter) != null }
                    .filter { component -> outputFilter == null || component.outputPins.pickFirstConnectableByType(outputFilter) != null }
                    .filter { "${it::class.simpleName}".lowercase().contains(filterRegex) }
                    .map { ComponentItem(it) }
                    .toTypedArray()
            )

            listView.items.addAll(
                *timelineWindow.graphs
                    .filter { "g${it.name.value}".lowercase().contains(filterRegex) }
                    .map { GraphItem(it) }
                    .toTypedArray()
            )

            val find = listView.items.find { it == selectedItem }
            if(find != null) {
                listView.selectionModel.select(find)
            } else {
                listView.selectionModel.select(listView.items.firstOrNull())
            }
        }

        textField.apply {
            addEventHandler(KeyEvent.KEY_PRESSED) { event ->
                if (event.code == KeyCode.ESCAPE) {
                    containerChildrens.remove(this@ComponentSelector)
                    event.consume()
                }
                if(event.code == KeyCode.DOWN) {
                    listView.selectionModel.selectNext()
                    event.consume()
                }
                if(event.code == KeyCode.UP) {
                    listView.selectionModel.selectPrevious()
                    event.consume()
                }
                if(event.code == KeyCode.ENTER) {
                    createSelected()
                    event.consume()
                }
            }
        }

        scrollPane.isFitToWidth = true

        top = textField
        center = scrollPane
        minWidth = 300.0
        relocate(x, y)
        containerChildrens.addAll(this)
        textField.requestFocus()
    }

    private fun createSelected() {
        val item = listView.selectionModel.selectedItem
        if (item is ComponentItem) {
            result(item.component.createSame())
            containerChildrens.remove(this@ComponentSelector)
        }
        if(item is GraphItem) {
            result(Graph().apply { graph = item.graph })
            containerChildrens.remove(this@ComponentSelector)
        }
    }

    class ComponentItem(val component: Component) {
        override fun toString(): String {
            return "${component::class.simpleName}"
        }
    }

    class GraphItem(val graph: ArrangementGraphsContainer) {
        override fun toString(): String {
            return "Graph: ${graph.name.value}"
        }
    }

    companion object {
        private val basicComponentTemplates = arrayListOf(
            SimpleParticlesGenerator(),
            ParticlesProduct(),
            MoveRotate(),
            Color(),
            ParticleRenderParams(),
            Slice(),
            ModFilter(),
            LineTest(),
            FuncLinear(),
            FuncSin(),
            FuncRandom()
        )
    }
}