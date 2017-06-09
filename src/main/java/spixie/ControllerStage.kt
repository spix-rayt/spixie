package spixie

import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.SplitPane
import javafx.scene.control.ToolBar
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane

class ControllerStage : BorderPane() {
    init {
        val splitPane = SplitPane()

        val componentListView = ListView<Component>()
        SplitPane.setResizableWithParent(componentListView, false)
        splitPane.orientation = Orientation.HORIZONTAL

        val componentBodyContainer = BorderPane()

        componentListView.items.addAll(Main.world.root)
        componentListView.selectionModel.selectedItemProperty().addListener { _, _, _ -> componentBodyContainer.center = componentListView.selectionModel.selectedItem.componentBody }


        componentListView.selectionModel.select(0)


        splitPane.items.addAll(componentListView, componentBodyContainer)
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
