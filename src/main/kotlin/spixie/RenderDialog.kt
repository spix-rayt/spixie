package spixie

import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window
import org.apache.commons.lang3.time.StopWatch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class RenderDialog(owner: Window): Stage() {
    init {
        val motionBlurSlider = Slider(1.0, 200.0, 1.0).apply {
            isShowTickMarks = true
            majorTickUnit = 1.0
            minorTickCount = 0
            isSnapToTicks = true
        }
        val progressBar = ProgressBar(0.0)
        val renderButton = Button("Render")
        val checkBoxAudio = CheckBox().apply { isSelected=true }
        val numberFps = NumberControl(24.0, "fps", 0.0).apply {
            limitMin(1.0)
            minNumberLineScale = 0.0
            maxNumberLineScale = 0.0
        }
        val remainingTimeLabel = Label("")

        val grid = GridPane()
        grid.add(
                Label().apply {
                    val textF = { v:Int -> "Motion Blur: ${if(v==1) "Off" else "x$v"}" }
                    motionBlurSlider.valueProperty().addListener { _, _, newValue ->
                        text = textF(newValue.toInt())
                    }
                    text = textF(motionBlurSlider.value.toInt())
                },
                0, 0
        )
        grid.add(motionBlurSlider, 1, 0)
        grid.add(Label("Audio"), 0, 1)
        grid.add(checkBoxAudio, 1, 1)
        grid.add(Label("fps"), 0, 2)
        grid.add(numberFps, 1, 2)
        grid.add(progressBar,0, 3, 2, 1)
        grid.add(remainingTimeLabel, 0, 4)
        grid.add(renderButton, 0, 5, 2, 1)

        grid.columnConstraints.addAll(ColumnConstraints(300.0), ColumnConstraints(500.0))
        grid.vgap = 8.0

        renderButton.apply {
            setOnAction {
                progressBar.progress = 0.0
                grid.isDisable = true
                val selectedBeats = timelineWindow.getSelectedBeats()
                val lastFramesRenderTime = mutableListOf<Double>()
                val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val zoneId = ZoneId.of("UTC")
                val stopWatch = StopWatch.createStarted()
                var lastRenderedFrame = 0
                renderManager.renderToFile(
                    { currentFrame, framesCount ->
                        progressBar.progress = currentFrame / framesCount.toDouble()
                        val t = stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000.0 / (currentFrame-lastRenderedFrame)
                        stopWatch.reset()
                        stopWatch.start()
                        if(lastRenderedFrame != 0) {
                            lastFramesRenderTime.add(t)
                            if(lastFramesRenderTime.size>10) {
                                lastFramesRenderTime.removeAt(0)
                                val average = lastFramesRenderTime.average()
                                val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(((framesCount - currentFrame) * average * 1000.0).roundToLong()), zoneId)
                                remainingTimeLabel.text = "${dateTimeFormatter.format(time)} (${(average*1000.0).roundToInt()} ms / frame)"
                            }
                        }
                        lastRenderedFrame = currentFrame
                    },
                    {
                        grid.isDisable = false
                    },
                    motionBlurSlider.value.toInt(),
                    selectedBeats.first,
                    selectedBeats.second,
                    checkBoxAudio.isSelected,
                    projectWindow.offset.value / projectWindow.bpm.value * 60.0,
                    numberFps.value.roundToInt()
                )
            }
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)
        }

        progressBar.apply {
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)
        }
        scene = Scene(grid)
        initOwner(owner)
        initModality(Modality.APPLICATION_MODAL)
        showAndWait()
    }
}