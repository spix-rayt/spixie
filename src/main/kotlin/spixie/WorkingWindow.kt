package spixie

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import java.util.*

class WorkingWindow : BorderPane() {
    private val menuBar = ToolBar()

    init {
        val renderButton = Button("Render")
        renderButton.onMouseClicked = EventHandler<MouseEvent> {
            this@WorkingWindow.isDisable = true
            Main.renderManager.renderToFile({ currentFrame, framesCount ->
                renderButton.text = currentFrame.toString() + " / " + framesCount
            }, {
                renderButton.text = "Render"
                this@WorkingWindow.isDisable = false
            })
        }
        renderButton.isFocusTraversable = false
        val slider = Slider(1.0,6.0,2.0).apply {
            isShowTickMarks = true
            majorTickUnit = 1.0
            minorTickCount = 0
            isSnapToTicks = true
            valueProperty().addListener { _, _, newValue ->
                Main.renderManager.scaleDown = newValue.toInt()
            }
            isFocusTraversable = false
        }

        val clearCacheButton = Button("Clear cache")
        clearCacheButton.setOnAction {
            Main.renderManager.clearCache()
        }
        clearCacheButton.isFocusTraversable = false

        val timeLabel = ValueLabel("Time")
        Main.renderManager.time.timeChanges.subscribe { time ->
            timeLabel.value = Math.round(time*1000)/1000.0
        }
        menuBar.items.addAll(renderButton, slider, clearCacheButton, timeLabel)

        setOnKeyPressed { event ->
            if(event.code == KeyCode.ESCAPE){
                if(centerStack.size > 0){
                    val last = centerStack.removeLast()
                    center = last as Node
                    center.requestFocus()
                    (center as? BorderPane)?.top = menuBar
                }
                event.consume()
            }
        }
    }

    private val centerStack = LinkedList<WorkingWindowOpenableContent>()

    fun open(workingWindowOpenableContent: WorkingWindowOpenableContent){
        if(center != null && center != workingWindowOpenableContent){
            centerStack.add(center as WorkingWindowOpenableContent)
        }
        center = workingWindowOpenableContent as Node
        center.requestFocus()
        (center as? BorderPane)?.top = menuBar
    }
}
