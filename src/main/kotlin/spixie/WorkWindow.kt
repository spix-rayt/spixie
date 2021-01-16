package spixie

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import java.util.*

class WorkWindow : BorderPane() {
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
                    showOpenDialog(this@WorkWindow.scene.window)?.let { file->
                        Core.audio.load(file)
                    }
                    center.requestFocus()
                }
            }
            isFocusTraversable = false
        }

        val renderButton = Button("Render")
        renderButton.setOnAction { RenderDialog(this.scene.window) }
        renderButton.isFocusTraversable = false
        val slider = Slider(0.0,3.0,1.0).apply {
            isShowTickMarks = true
            majorTickUnit = 1.0
            minorTickCount = 0
            isSnapToTicks = true
            valueProperty().addListener { _, _, newValue ->
                Core.renderManager.scaleDown = 1 shl newValue.toInt()
            }
            isFocusTraversable = false
        }

        val timeLabel = Label().apply {
            Core.renderManager.time.timeChanges.subscribe { time ->
                text = "Time: ${String.format("%.3f", Math.round(time*1000)/1000.0)}"
            }
        }

        val lastRenderInfo = Label()
        Core.renderManager.lastRenderInfoSubject.observeOn(JavaFxScheduler.platform()).subscribe {
            lastRenderInfo.text = it
        }
        menuBar.items.addAll(importAudioButton, renderButton, slider, timeLabel, Core.renderManager.bpm, Core.renderManager.offset, Pane().apply { HBox.setHgrow(this, Priority.ALWAYS) }, lastRenderInfo)

//        menuBar.addEventHandler(KeyEvent.ANY){ event ->
//            center.fireEvent(event.copyFor(center, center))
//            event.consume()
//        }

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

    private val centerStack = LinkedList<OpenableContent>()

    fun open(openableContent: OpenableContent){
        if(center != null && center != openableContent){
            centerStack.add(center as OpenableContent)
        }
        center = openableContent as Node
        center.requestFocus()
    }

    interface OpenableContent
}
