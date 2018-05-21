package spixie.visualEditor


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

    init {
        treeView.apply {
            val basicItems = TreeItem<Any>("Basic")
            val moduleItems = TreeItem<Any>("Module")
            val graphItems = TreeItem<Any>("Graphs")
            root.children.addAll(basicItems, moduleItems)
            basicItems.children.add(TreeItem(ComponentListItem(SimpleParticlesGenerator::class.java)))
            basicItems.children.add(TreeItem(ComponentListItem(ParticlesProduct::class.java)))
            basicItems.children.add(TreeItem(ComponentListItem(MoveRotate::class.java)))
            basicItems.children.add(TreeItem(ComponentListItem(Color::class.java)))
            basicItems.children.add(TreeItem(ComponentListItem(Slice::class.java)))

            moduleItems.children.setAll(Main.arrangementWindow.visualEditor.modules.filter { !it.isMain }.map { TreeItem<Any>(it) })

            if(forMain) {
                Main.arrangementWindow.graphs.forEach { graph->
                    graphItems.children.add(TreeItem(ComponentListGraphItem(graph)))
                }
                root.children.addAll(graphItems)
            }

            expandChildItems(root)

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

        textField.apply {
            setOnKeyPressed { event ->
                if (event.code == KeyCode.ESCAPE) {
                    containerChildrens.remove(this@ComponentsList)
                    event.consume()
                }
                if(event.code == KeyCode.DOWN){
                    treeView.selectionModel.selectNext()
                }
                if(event.code == KeyCode.UP){
                    treeView.selectionModel.selectPrevious()
                }
                if(event.code == KeyCode.ENTER){
                    createSelected()
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

    private fun expandChildItems(item: TreeItem<*>){
        item.isExpanded = true
        item.children.forEach {
            if(!it.isLeaf){
                expandChildItems(it)
            }
        }
    }
}