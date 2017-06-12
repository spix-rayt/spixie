package spixie

import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import spixie.components.Multiplier

class ControllerStage : BorderPane() {
    init {
        val splitPane = SplitPane()

        val componentTreeView = TreeView<ComponentsListItem>()
        SplitPane.setResizableWithParent(componentTreeView, false)
        splitPane.orientation = Orientation.HORIZONTAL

        val componentBodyContainer = BorderPane()

        componentTreeView.root = TreeItem()
        componentTreeView.isShowRoot = false
        componentTreeView.root.children.add(Main.world.root)
        componentTreeView.selectionModel.selectedItemProperty().addListener { _, _, _ -> componentBodyContainer.center = componentTreeView.selectionModel.selectedItem.value.getPropsPane() }


        componentTreeView.selectionModel.select(0)

        var contextMenu = ContextMenu()

        componentTreeView.setOnContextMenuRequested { event ->
            val selectedTreeItem = componentTreeView.selectionModel.selectedItem
            val selectedValue = selectedTreeItem.value
            when(selectedValue){
                is Component -> {
                    contextMenu.hide()
                    contextMenu = ContextMenu()
                    val menuInsert = Menu("Insert")

                    val menuItem1 = MenuItem("Multiplier")
                    menuInsert.items.addAll(menuItem1)

                    contextMenu.items.addAll(menuInsert)


                    menuItem1.setOnAction { event ->
                        val component = Multiplier()
                        selectedTreeItem.children.addAll(
                                TreeItem<ComponentsListItem>(
                                        ComponentObject(component, component.genPropsPane())
                                )
                        )
                    }
                    contextMenu.show(componentTreeView,event.screenX, event.screenY)
                }
            }
        }


        splitPane.items.addAll(componentTreeView, componentBodyContainer)
        splitPane.setDividerPosition(0, 0.15)

        center = splitPane

        val menuBar = ToolBar()
        val renderButton = Button("Render")
        renderButton.onMouseClicked = EventHandler<MouseEvent> {
            this@ControllerStage.isDisable = true
            Main.world.renderToFile(object: World.FrameRenderedToFileEvent{
                override fun handle(currentFrame: Int, framesCount: Int) {
                    renderButton.text = currentFrame.toString() + " / " + framesCount
                }
            }, object: World.RenderToFileCompleted{
                override fun handle() {
                    renderButton.text = "Render"
                    this@ControllerStage.isDisable = false
                }
            })
        }
        menuBar.items.addAll(renderButton, Main.world.frame)
        top = menuBar
    }
}
