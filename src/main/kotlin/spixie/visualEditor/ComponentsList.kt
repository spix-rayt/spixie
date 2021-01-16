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
import spixie.Core
import spixie.arrangement.ArrangementGraphsContainer
import spixie.visualEditor.components.*

class ComponentsList(x: Double, y:Double, private val containerChildrens: ObservableList<Node>, private val result: (component: Component) -> Unit): BorderPane() {
    private val listView = ListView<Any>().apply {
        setOnMouseClicked {
            createSelected()
        }
    }

    private val scrollPane = ScrollPane(listView)

    private val textField = TextField()

    private val basicItemsList = listOf(
            ComponentListItem(SimpleParticlesGenerator::class.java),
            ComponentListItem(ParticlesProduct::class.java),
            ComponentListItem(MoveRotate::class.java),
            ComponentListItem(Color::class.java),
            ComponentListItem(Slice::class.java),
            ComponentListItem(ModFilter::class.java),
            ComponentListItem(LineTest::class.java),
            ComponentListItem(Render::class.java),
            ComponentListItem(FuncLinear::class.java),
            ComponentListItem(FuncSin::class.java),
            ComponentListItem(FuncRandom::class.java),
            ComponentListItem(ParticleTransformer::class.java)
    )

    init {
        textField.focusedProperty().addListener { _, _, newValue ->
            if(!newValue){
                scene?.let { scene ->
                    if(scene.focusOwner == scrollPane || scene.focusOwner == listView){
                        textField.requestFocus()
                    }else{
                        containerChildrens.remove(this@ComponentsList)
                    }
                }
            }
        }

        JavaFxObservable.valuesOf(textField.textProperty()).startWith(textField.text).subscribe { filterText->
            val filterRegex = Regex(filterText.toLowerCase().map { ".*$it" }.joinToString(separator = "") + ".*")
            val selectedItem = listView.selectionModel.selectedItem
            listView.items.clear()
            listView.items.addAll(
                    *basicItemsList
                            .filter { it.clazz.simpleName.toLowerCase().contains(filterRegex) }
                            .toTypedArray()
            )

            listView.items.addAll(
                    *Core.arrangementWindow.graphs
                            .filter { ("g"+it.name.value).toLowerCase().contains(filterRegex) }
                            .map { ComponentListGraphItem(it) }
                            .toTypedArray()
            )

            val find = listView.items.find { it == selectedItem }
            if(find != null){
                listView.selectionModel.select(find)
            }else{
                listView.selectionModel.select(listView.items.firstOrNull())
            }
        }

        textField.apply {
            addEventHandler(KeyEvent.KEY_PRESSED) { event ->
                if (event.code == KeyCode.ESCAPE) {
                    containerChildrens.remove(this@ComponentsList)
                    event.consume()
                }
                if(event.code == KeyCode.DOWN){
                    listView.selectionModel.selectNext()
                    event.consume()
                }
                if(event.code == KeyCode.UP){
                    listView.selectionModel.selectPrevious()
                    event.consume()
                }
                if(event.code == KeyCode.ENTER){
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

    private fun createSelected(){
        val value = listView.selectionModel.selectedItem
        if (value is ComponentListItem) {
            val component = value.clazz.newInstance() as @kotlin.ParameterName(name = "component") Component
            result(component)
            containerChildrens.remove(this@ComponentsList)
        }
        if(value is ComponentListGraphItem){
            result(Graph().apply { graph = value.graph })
            containerChildrens.remove(this@ComponentsList)
        }
    }

    class ComponentListGraphItem(val graph: ArrangementGraphsContainer) {
        override fun toString(): String {
            return "Graph: ${graph.name.value}"
        }
    }

    class ComponentListItem(val clazz: Class<*>) {
        override fun toString(): String {
            return clazz.simpleName
        }
    }
}