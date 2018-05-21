package spixie

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.util.Duration
import java.io.File
import java.io.ObjectInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.*

class Main : Application() {
    @Throws(Exception::class)
    override fun start(stage: Stage) {
        val root = Group()
        val imageView = ImageView().apply {
            style="-fx-background:transparent;"
            isSmooth = true
            isPreserveRatio = false
            isCache = false
            cacheHint = CacheHint.SPEED
        }
        val imagePane = Pane()

        imagePane.children.addAll(imageView)
        root.children.addAll(imagePane)
        imagePane.style = "-fx-background-color: #000000;"

        val scene = Scene(root, 100.0, 100.0)
        scene.stylesheets.add("style.css")
        scene.focusOwnerProperty().addListener { _, _, newValue ->
            if(newValue == null){
                Main.workingWindow.center.requestFocus()
            }
        }
        imageView.fitWidthProperty().bind(scene.widthProperty())
        imageView.fitHeightProperty().bind(scene.heightProperty())

        stage.apply {
            title = "Render"
            this.scene = scene
            isMaximized = true
            fullScreenExitKeyCombination = KeyCombination.NO_MATCH
            show()
            isFullScreen = true
        }

        workingWindow.prefWidthProperty().bind(scene.widthProperty())
        workingWindow.prefHeightProperty().bind(scene.heightProperty())
        root.children.addAll(workingWindow)
        workingWindow.open(arrangementWindow)

        val windowOpacity = arrayOf(1.0f)
        workingWindow.style = "-fx-opacity: " + windowOpacity[0]

        var playStartTime = 0.0

        root.onKeyPressed = EventHandler<KeyEvent> { event ->
            if(event.isControlDown && !event.isAltDown && !event.isShiftDown){
                if (event.code == KeyCode.DIGIT1 && event.isControlDown) {
                    windowOpacity[0] = 1.0f
                    workingWindow.style = "-fx-opacity: " + windowOpacity[0]
                }
                if (event.code == KeyCode.DIGIT2 && event.isControlDown) {
                    windowOpacity[0] = 0.8f
                    workingWindow.style = "-fx-opacity: " + windowOpacity[0]
                }
                if (event.code == KeyCode.DIGIT3 && event.isControlDown) {
                    windowOpacity[0] = 0.6f
                    workingWindow.style = "-fx-opacity: " + windowOpacity[0]
                }
                if (event.code == KeyCode.DIGIT4 && event.isControlDown) {
                    windowOpacity[0] = 0.4f
                    workingWindow.style = "-fx-opacity: " + windowOpacity[0]
                }
                if (event.code == KeyCode.DIGIT5 && event.isControlDown) {
                    windowOpacity[0] = 0.2f
                    workingWindow.style = "-fx-opacity: " + windowOpacity[0]
                }
                if (event.code == KeyCode.DIGIT6 && event.isControlDown) {
                    windowOpacity[0] = 0.0f
                    workingWindow.style = "-fx-opacity: " + windowOpacity[0]
                }
            }

            if(!event.isControlDown && !event.isAltDown && !event.isShiftDown){
                if (event.code == KeyCode.TAB) {
                    workingWindow.style = "-fx-opacity: 0.0"
                }

                if(event.code == KeyCode.A){
                    renderManager.time.frame = (renderManager.time.frame-1).coerceAtLeast(0)
                }
                if(event.code == KeyCode.D){
                    renderManager.time.frame += 1
                }
                if(event.code == KeyCode.P){
                    if(renderManager.autoRenderNextFrame != true){
                        renderManager.autoRenderNextFrame = true
                        renderManager.time.frame = renderManager.time.frame
                        renderManager.requestRender()
                    }
                }
                if(event.code == KeyCode.SPACE){
                    if(audio.isPlaying()){
                        audio.pause()
                        Platform.runLater { renderManager.time.time = playStartTime }
                    }else{
                        playStartTime = renderManager.time.time
                        audio.play(Duration.seconds(Math.round((playStartTime-Main.renderManager.offset.value)*3600/Main.renderManager.bpm.value)/60.0))
                    }
                }
            }
        }

        root.onKeyReleased = EventHandler<KeyEvent> { event ->
            if (event.code == KeyCode.TAB) {
                workingWindow.style = "-fx-opacity: " + windowOpacity[0]
            }
            if(event.code == KeyCode.P){
                renderManager.autoRenderNextFrame = false
            }
        }

        stage.onCloseRequest = EventHandler<WindowEvent> {
            val bytes = arrangementWindow.save()
            Files.move(Paths.get("save.spixie"), Paths.get("save${SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(Calendar.getInstance().time)}.spixie"), StandardCopyOption.REPLACE_EXISTING)
            Files.write(Paths.get("save.spixie"), bytes)
            Platform.exit()
        }

        File("save.spixie").let {
            if(it.exists()){
                arrangementWindow.load(ObjectInputStream(it.inputStream()))
            }
        }

        renderManager.renderStart(imageView)
        object : AnimationTimer() {
            override fun handle(now: Long) {
                renderManager.perFrame()
                if(workingWindow.opacity != 0.0){
                    arrangementWindow.perFrame()
                }
            }
        }.start()
    }

    companion object {
        var renderManager = RenderManager()
        val workingWindow = WorkingWindow()
        val arrangementWindow = ArrangementWindow()
        val audio = Audio()

        var dragAndDropObject: Any = Any()
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java)
}
