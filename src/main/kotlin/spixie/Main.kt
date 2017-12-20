package spixie

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

class Main : Application() {
    @Throws(Exception::class)
    override fun start(stage: Stage) {
        val root = Group()
        val imageView = ImageView()
        val imagePane = Pane()
        imageView.style="-fx-background:transparent;"
        imageView.isSmooth = true
        imageView.isPreserveRatio = false
        imageView.isCache = false
        imageView.cacheHint = CacheHint.SPEED


        imagePane.children.addAll(imageView)
        root.children.addAll(imagePane)
        imagePane.style = "-fx-background-color: #000000;"

        val scene = Scene(root, 100.0, 100.0)
        scene.stylesheets.add("style.css")
        imageView.fitWidthProperty().bind(scene.widthProperty())
        imageView.fitHeightProperty().bind(scene.heightProperty())
        stage.title = "Render"
        stage.scene = scene
        stage.isMaximized = true
        stage.fullScreenExitKeyCombination = KeyCombination.NO_MATCH
        stage.show()
        stage.isFullScreen = true

        workingWindow.prefWidthProperty().bind(scene.widthProperty())
        workingWindow.prefHeightProperty().bind(scene.heightProperty())
        root.children.addAll(workingWindow)

        val windowOpacity = arrayOf(1.0f)
        workingWindow.style = "-fx-opacity: " + windowOpacity[0]

        var playStartTime = 0.0

        root.onKeyPressed = EventHandler<KeyEvent> { event ->
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
            if (event.code == KeyCode.TAB) {
                workingWindow.style = "-fx-opacity: 0.0"
            }

            if(event.code == KeyCode.A){
                val frame = world.time.frame
                if(frame>0) world.time.frame -= 1
            }
            if(event.code == KeyCode.D){
                world.time.frame += 1
            }
            if(event.code == KeyCode.P){
                world.autoRenderNextFrame = true
            }
            if(event.code == KeyCode.SPACE){
                if(audio.isPlayed()){
                    audio.pause()
                    Platform.runLater { world.time.time = playStartTime }
                }else{
                    playStartTime = world.time.time
                    audio.play(Duration.seconds(world.time.frame/60.0))
                }
            }
            if(event.code == KeyCode.C){
                workingWindow.arrangementWindow.timePointerCentering = true
            }
            if(event.code == KeyCode.V){
                Main.workingWindow.nextOpen(Main.workingWindow.arrangementWindow.visualEditor)
            }
            if(event.code == KeyCode.Q){
                Main.workingWindow.arrangementWindow.selectionBlock.buildGraph()
            }
        }

        root.onKeyReleased = EventHandler<KeyEvent> { event ->
            if (event.code == KeyCode.TAB) {
                workingWindow.style = "-fx-opacity: " + windowOpacity[0]
            }
            if(event.code == KeyCode.P){
                world.autoRenderNextFrame = false
            }
            if(event.code == KeyCode.C){
                workingWindow.arrangementWindow.timePointerCentering = false
            }
        }

        stage.onCloseRequest = EventHandler<WindowEvent> {
            Main.world.currentRenderThread.interrupt()
            Platform.exit()
        }

        world.renderStart(imageView)
    }

    companion object {
        var world: World = World()
        val workingWindow = WorkingWindow()
        val audio = Audio()
        val settings = Settings.load()

        var internalObject: Any = Any()

        @JvmStatic fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}
