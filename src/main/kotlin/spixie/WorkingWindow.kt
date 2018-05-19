package spixie

import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Slider
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import java.util.*

class WorkingWindow : BorderPane() {
    init {
        val menuBar = ToolBar()
        val importAudioButton = Button("Import audio").apply {
            setOnAction {
                FileChooser().apply {
                    title="Select audio file"
                    extensionFilters.addAll(
                            FileChooser.ExtensionFilter("mp3", "*.mp3"),
                            FileChooser.ExtensionFilter("All files", "*.*")
                    )
                    showOpenDialog(this@WorkingWindow.scene.window)?.let { file->
                        Main.audio.load(file)
                    }
                    center.requestFocus()
                }
            }
            isFocusTraversable = false
        }


        val renderButton = Button("Render")
        renderButton.setOnAction { RenderDialog(this.scene.window) }
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

        val timeLabel = ValueLabel("Time")
        Main.renderManager.time.timeChanges.subscribe { time ->
            timeLabel.value = Math.round(time*1000)/1000.0
        }
        menuBar.items.addAll(importAudioButton, renderButton, slider, timeLabel, Main.renderManager.bpm, Main.renderManager.offset)

        menuBar.addEventHandler(KeyEvent.ANY){ event ->
            center.fireEvent(event.copyFor(center, center))
            event.consume()
        }

        top = menuBar

        setOnKeyPressed { event ->
            if(event.code == KeyCode.ESCAPE){
                if(centerStack.size > 0){
                    val last = centerStack.removeLast()
                    center = last as Node
                    center.requestFocus()
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
    }
}
