package spixie.visualEditor


import io.reactivex.rxjavafx.observables.JavaFxObservable
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import spixie.Main
import spixie.visualEditor.components.*

class ComponentsList(x: Double, y:Double, private val containerChildrens: ObservableList<Node>, forMain: Boolean, private val result: (component: Component) -> Unit): BorderPane() {
    private val treeView = TreeView<Any>(TreeItem("Components"))
    private val scrollPane = ScrollPane(treeView)
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
            ComponentListItem(Render::class.java),
            ComponentListItem(RenderDepth::class.java),
            ComponentListItem(FocusBlur::class.java)
    )

    val basicItems = TreeItem<Any>("Basic").apply { isExpanded = true }
    val moduleItems = TreeItem<Any>("Module").apply { isExpanded = true }
    val graphItems = TreeItem<Any>("Graphs").apply { isExpanded = true }

    init {
        treeView.apply {
            root.children.addAll(basicItems, moduleItems)
            if(forMain) {

                root.children.addAll(graphItems)
            }
            root.isExpanded = true

            setOnMouseClicked {
                createSelected()
            }
        }

        textField.focusedProperty().addListener { _, _, newValue ->
            if(!newValue){
                scene?.let { scene ->
                    if(scene.focusOwner == scrollPane || scene.focusOwner == treeView){
                        textField.requestFocus()
                    }else{
                        containerChildrens.remove(this@ComponentsList)
                    }
                }
            }
        }

        JavaFxObservable.valuesOf(textField.textProperty()).startWith(textField.text).subscribe { filterText->
            val filterRegex = Regex(filterText.toLowerCase().map { ".*$it" }.joinToString(separator = "") + ".*")
            val selectedItem = treeView.selectionModel.selectedItem
            basicItems.children.setAll(
                    basicItemsList
                            .filter { it.clazz.simpleName.toLowerCase().contains(filterRegex) }
                            .map { TreeItem<Any>(it) }
            )

            moduleItems.children.setAll(
                    Main.arrangementWindow.visualEditor.modules
                            .filter { it.name.toLowerCase().contains(filterRegex) }
                            .filter { !it.isMain }
                            .map { TreeItem<Any>(it) }
            )

            graphItems.children.setAll(
                    Main.arrangementWindow.graphs
                            .filter { it.name.value.toLowerCase().contains(filterRegex) }
                            .map { TreeItem<Any>(ComponentListGraphItem(it)) }
            )

            val allSelectableItems = (basicItems.children + moduleItems.children + graphItems.children)
            val find = allSelectableItems.find { it.value == selectedItem?.value }
            if(find != null){
                treeView.selectionModel.select(find)
            }else{
                treeView.selectionModel.select(allSelectableItems.firstOrNull())
            }
        }

        textField.apply {
            setOnKeyPressed { event ->
                if (event.code == KeyCode.ESCAPE) {
                    containerChildrens.remove(this@ComponentsList)
                    event.consume()
                }
                if(event.code == KeyCode.DOWN){
                    treeView.selectionModel.select(treeView.selectionModel.selectedIndex+1)
                    event.consume()
                }
                if(event.code == KeyCode.UP){
                    treeView.selectionModel.select((treeView.selectionModel.selectedIndex-1).coerceAtLeast(0))
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
        val value = treeView.selectionModel.selectedItem?.value
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