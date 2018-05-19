package spixie

import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window

class RenderDialog(owner: Window): Stage() {
    init {
        val motionBlurSlider = Slider(1.0, 20.0, 2.0).apply {
            isShowTickMarks = true
            majorTickUnit = 1.0
            minorTickCount = 0
            isSnapToTicks = true
        }
        val progressBar = ProgressBar(0.0)
        val renderButton = Button("Render")
        val checkBoxAudio = CheckBox().apply { isSelected=true }
        val grid = GridPane()

        grid.add(
                Label().apply {
                    val textF = { v:Int -> "Motion Blur: ${if(v==1) "Off" else "x$v slower render"}" }
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
        grid.add(progressBar,0, 2, 2, 1)
        grid.add(renderButton, 0, 3, 2, 1)

        grid.columnConstraints.addAll(ColumnConstraints(300.0), ColumnConstraints(500.0))
        grid.vgap = 8.0

        renderButton.apply {
            setOnAction {
                progressBar.progress = 0.0
                grid.isDisable = true
                val selectedFrames = Main.arrangementWindow.getSelectedFrames()
                Main.renderManager.renderToFile(
                        { currentFrame, framesCount ->
                            progressBar.progress = currentFrame / framesCount.toDouble()
                        },
                        {
                            grid.isDisable = false
                        },
                        motionBlurSlider.value.toInt(),
                        selectedFrames.first,
                        selectedFrames.second,
                        checkBoxAudio.isSelected,
                        Main.renderManager.offset.value/Main.renderManager.bpm.value*60.0
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