package spixie.visualEditor


import io.reactivex.rxjavafx.observables.JavaFxObservable
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import spixie.Main
import spixie.visualEditor.components.*
import spixie.visualEditor.components.transformers.*

class ComponentsList(x: Double, y:Double, private val containerChildrens: ObservableList<Node>, forMain: Boolean, private val result: (component: Component) -> Unit): BorderPane() {
    private val listView = ListView<Any>()
    private val scrollPane = ScrollPane(listView)
    private val textField = TextField()
    private val basicItemsList = listOf(
            ComponentListItem(SimpleParticlesGenerator::class.java),
            ComponentListItem(ParticlesProduct::class.java),
            ComponentListItem(ParticlesPower::class.java),
            ComponentListItem(MoveRotate::class.java),
            ComponentListItem(Color::class.java),
            ComponentListItem(Slice::class.java),
            ComponentListItem(LineTest::class.java),
            ComponentListItem(SizeTransformer::class.java),
            ComponentListItem(GlowTransformer::class.java),
            ComponentListItem(EdgeTransformer::class.java),
            ComponentListItem(ScaleTransformer::class.java),
            ComponentListItem(RotateTransformer::class.java),
            ComponentListItem(PositionTransformer::class.java),
            ComponentListItem(Render::class.java)
    )

    init {
        listView.apply {
            setOnMouseClicked {
                createSelected()
            }
        }

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
                    *Main.arrangementWindow.visualEditor.modules
                            .filter { it.name.toLowerCase().contains(filterRegex) }
                            .filter { !it.isMain }
                            .toTypedArray()
            )

            if(forMain){
                listView.items.addAll(
                        *Main.arrangementWindow.graphs
                                .filter { ("g"+it.name.value).toLowerCase().contains(filterRegex) }
                                .map { ComponentListGraphItem(it) }
                                .toTypedArray()
                )
            }

            val find = listView.items.find { it == selectedItem }
            if(find != null){
                listView.selectionModel.select(find)
            }else{
                listView.selectionModel.select(listView.items.firstOrNull())
            }
        }

        textField.apply {
            setOnKeyPressed { event ->
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
        minWidth = 600.0
        relocate(x, y)
        containerChildrens.addAll(this)
        textField.requestFocus()
    }

    private fun createSelected(){
        val value = listView.selectionModel.selectedItem
        if (value is ComponentListItem) {
            result(value.clazz.newInstance() as @kotlin.ParameterName(name = "component") Component)
            containerChildrens.remove(this@ComponentsList)
        }
        if(value is Module){
            result(ModuleComponent().apply { module = value })
            containerChildrens.remove(this@ComponentsList)
        }
        if(value is ComponentListGraphItem){
            result(Graph(value.graph))
            containerChildrens.remove(this@ComponentsList)
        }
    }
}