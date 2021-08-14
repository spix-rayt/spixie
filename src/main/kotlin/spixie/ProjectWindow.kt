package spixie

import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
import spixie.visualEditor.Module
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ProjectWindow : BorderPane() {
    val bpm = NumberControl(160.0, "BPM", 0.0).apply {
        minNumberLineScale = 0.0
        maxNumberLineScale = 0.0
        limitMin(60.0)
        limitMax(999.0)
    }

    val fps = NumberControl(24.0, "FPS")

    val offset = NumberControl(0.0, "Offset").apply {
        changes.subscribe {
            timelineWindow.spectrogram.requestRedraw()
        }
    }

    val timeHolder = TimeHolder(bpm, fps, offset)

    val samplesPerPixel = NumberControl(20.0, "SPP", 0.0).apply {
        limitMin(1.0)
        limitMax(5000.0)
        minNumberLineScale = 0.0
        maxNumberLineScale = 0.0
        changes.subscribe {
            renderManager.requestRender()
        }
    }

    val lastRenderInfo = Label()

    init {
        val topMenuBar = ToolBar()
        val importAudioButton = Button("Import audio").apply {
            setOnAction {
                FileChooser().apply {
                    title = "Select audio file"
                    extensionFilters.addAll(
                            FileChooser.ExtensionFilter("mp3", "*.mp3"),
                            FileChooser.ExtensionFilter("All files", "*.*")
                    )
                    showOpenDialog(this@ProjectWindow.scene.window)?.let { file->
                        audio.load(file)
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
                renderManager.scaleDown = 1 shl newValue.toInt()
            }
            isFocusTraversable = false
        }

        val timeLabel = Label().apply {
            timeHolder.timeChanges.subscribe { time ->
                text = "Time: ${String.format("%.3f", (time * 1000).roundToInt() /1000.0)}"
            }
        }

        topMenuBar.items.addAll(
            importAudioButton,
            renderButton,
            slider,
            timeLabel,
            bpm,
            offset,
            samplesPerPixel,
            Pane().apply { HBox.setHgrow(this, Priority.ALWAYS) },
            lastRenderInfo
        )

//        menuBar.addEventHandler(KeyEvent.ANY) { event ->
//            center.fireEvent(event.copyFor(center, center))
//            event.consume()
//        }

        top = topMenuBar

        setOnKeyPressed { event ->
            if(event.code == KeyCode.ESCAPE) {
                if(centerStack.size > 0) {
                    val last = centerStack.removeLast()
                    center = last as Node
                    center.requestFocus()
                }
                event.consume()
            }
        }
    }

    private val centerStack = LinkedList<OpenableContent>()

    fun open(openableContent: OpenableContent) {
        if(center != null && center != openableContent) {
            centerStack.add(center as OpenableContent)
        }
        center = openableContent as Node
        center.requestFocus()
    }

    interface OpenableContent

    fun deserializeAndLoad(json: String) {
        val serializedProject = Gson().fromJson(json, SerializedProject::class.java)
        visualEditor.setAsMainModule(Module.fromSerialized(serializedProject.mainModule))
        projectWindow.fps.value = serializedProject.fps
        projectWindow.bpm.value = serializedProject.bpm
        projectWindow.offset.value = serializedProject.offset
    }

    fun serialize(): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(
            SerializedProject(
                visualEditor.mainModule.toSerialized(),
                fps.value,
                bpm.value,
                offset.value
            )
        )
    }

    data class SerializedProject(
        val mainModule: Module.SerializedModule,
        val fps: Double,
        val bpm: Double,
        val offset: Double
    )

    fun saveProject() {
        val json = serialize()
        if(!File("save/").exists()) File("save/").mkdir()
        if(File("save/save.spixie").exists()) {
            Files.move(Paths.get("save/save.spixie"), Paths.get("save/save${SimpleDateFormat("yyyy-MM-dd_HHmmss").format(Calendar.getInstance().time)}.spixie"), StandardCopyOption.REPLACE_EXISTING)
        }
        File("save/save.spixie").writeText(json)
    }

    fun loadLastProject() {
        File("save/save.spixie").let {
            if(it.exists()) {
                deserializeAndLoad(it.readText())
            }
        }
    }
}
