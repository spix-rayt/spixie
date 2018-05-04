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
        val menuBar = ToolBar()
        val renderButton = Button("Render")
        renderButton.onMouseClicked = EventHandler<MouseEvent> {
            this@WorkingWindow.isDisable = true
            Main.renderManager.renderToFile(object: RenderManager.FrameRenderedToFileEvent{
                override fun handle(currentFrame: Int, framesCount: Int) {
                    renderButton.text = currentFrame.toString() + " / " + framesCount
                }
            }, object: RenderManager.RenderToFileCompleted{
                override fun handle() {
                    renderButton.text = "Render"
                    this@WorkingWindow.isDisable = false
                }
            })
        }
        renderButton.isFocusTraversable = false
        val slider = Slider(1.0,6.0,2.0)
        slider.isShowTickMarks = true
        slider.majorTickUnit = 1.0
        slider.minorTickCount = 0
        slider.isSnapToTicks = true
        slider.valueProperty().addListener { _, _, newValue ->
            Main.renderManager.scaleDown = newValue.toInt()
        }
        slider.isFocusTraversable = false

        val clearCacheButton = Button("Clear cache")
        clearCacheButton.setOnAction {
            Main.renderManager.clearCache()
        }
        clearCacheButton.isFocusTraversable = false

        val timeLabel = ValueLabel("Time")
        Main.renderManager.time.timeChanges.subscribe { time ->
            timeLabel.value.value = Math.round(time*1000)/1000.0
        }
        menuBar.items.addAll(renderButton, slider, clearCacheButton, timeLabel)
        top = menuBar


        setOnKeyPressed { event ->
            if(event.code == KeyCode.ESCAPE){
                if(centerStack.size > 0){
                    val last = centerStack.removeLast()
                    center = last as Node
                }
                event.consume()
            }
        }

        isFocusTraversable = true
        isFocused = true
        nextOpen(arrangementWindow)
    }

    val centerStack = LinkedList<WorkingWindowOpenableContent>()

    fun nextOpen(workingWindowOpenableContent: WorkingWindowOpenableContent){
        if(center != null){
            centerStack.add(center as WorkingWindowOpenableContent)
        }
        center = workingWindowOpenableContent as Node
        center.requestFocus()
    }
}
