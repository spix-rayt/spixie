package spixie.visual_editor


import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import spixie.visual_editor.components.Color
import spixie.visual_editor.components.Graph
import spixie.visual_editor.components.Test

class ComponentsList(x: Double, y:Double, val containerChildrens: ObservableList<Node>, val result: (component: Component) -> Unit): BorderPane() {
    val treeView = TreeView<Any>(TreeItem("Components"))
    val scrollPane = ScrollPane(treeView)
    val textField = TextField()

    init {
        treeView.apply {
            root.children.add(TreeItem(ComponentListItem(Test::class.java)))
            root.children.add(TreeItem(ComponentListItem(Color::class.java)))
            root.children.add(TreeItem(ComponentListItem(Graph::class.java)))
            expandChildItems(root)

            focusedProperty().addListener { _, _, newValue ->
                if (newValue) {
                    textField.requestFocus()
                }
            }

            setOnMouseClicked {
                createSelected()
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

        scrollPane.focusedProperty().addListener { _, _, newValue ->
            if(newValue){
                textField.requestFocus()
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

    fun createSelected(){
        val value = treeView.selectionModel.selectedItem?.value
        if (value is ComponentListItem) {
            result(value.clazz.newInstance() as @kotlin.ParameterName(name = "component") Component)
            containerChildrens.remove(this@ComponentsList)
        }
    }

    fun expandChildItems(item: TreeItem<*>){
        item.isExpanded = true
        item.children.forEach {
            if(!it.isLeaf){
                expandChildItems(it)
            }
        }
    }
}