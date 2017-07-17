package spixie

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import java.util.*

class WorkingWindow : BorderPane() {
    val arrangementWindow = ArrangementWindow()
    init {
        center = arrangementWindow

        val menuBar = ToolBar()
        val renderButton = Button("Render")
        renderButton.onMouseClicked = EventHandler<MouseEvent> {
            this@WorkingWindow.isDisable = true
            Main.world.renderToFile(object: World.FrameRenderedToFileEvent{
                override fun handle(currentFrame: Int, framesCount: Int) {
                    renderButton.text = currentFrame.toString() + " / " + framesCount
                }
            }, object: World.RenderToFileCompleted{
                override fun handle() {
                    renderButton.text = "Render"
                    this@WorkingWindow.isDisable = false
                }
            })
        }
        val slider = Slider(1.0,6.0,2.0)
        slider.isShowTickMarks = true
        slider.majorTickUnit = 1.0
        slider.minorTickCount = 0
        slider.isSnapToTicks = true
        slider.valueProperty().addListener { _, _, newValue ->
            Main.world.scaleDown = newValue.toInt()
        }

        val clearCacheButton = Button("Clear cache")
        clearCacheButton.setOnAction {
            Main.world.clearCache()
        }
        menuBar.items.addAll(renderButton, slider, clearCacheButton)
        top = menuBar


        setOnKeyPressed { event ->
            if(event.code == KeyCode.ESCAPE){
                if(centerStack.size > 0){
                    val last = centerStack.removeLast()
                    center = last as Node
                }
            }
        }
    }

    val centerStack = LinkedList<WorkingWindowOpenableContent>()

    fun nextOpen(workingWindowOpenableContent: WorkingWindowOpenableContent){
        centerStack.add(center as WorkingWindowOpenableContent)
        center = workingWindowOpenableContent as Node
    }
}
