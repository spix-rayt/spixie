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
                if(event.code == KeyCode.C){
                    Main.workingWindow.arrangementWindow.selectionBlock.copy()
                }
                if(event.code == KeyCode.V){
                    Main.workingWindow.arrangementWindow.selectionBlock.paste()
                }
                if(event.code == KeyCode.D){
                    Main.workingWindow.arrangementWindow.selectionBlock.duplicate()
                }
            }

            if(!event.isControlDown && !event.isAltDown && !event.isShiftDown){
                if (event.code == KeyCode.TAB) {
                    workingWindow.style = "-fx-opacity: 0.0"
                }

                if(event.code == KeyCode.A){
                    val frame = renderManager.time.frame
                    if(frame>0) renderManager.time.frame -= 1
                }
                if(event.code == KeyCode.D){
                    renderManager.time.frame += 1
                }
                if(event.code == KeyCode.P){
                    renderManager.autoRenderNextFrame = true
                    renderManager.time.frame = renderManager.time.frame
                }
                if(event.code == KeyCode.SPACE){
                    if(audio.isPlaying()){
                        audio.pause()
                        Platform.runLater { renderManager.time.time = playStartTime }
                    }else{
                        playStartTime = renderManager.time.time
                        audio.play(Duration.seconds(renderManager.time.frame/60.0))
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
                if(event.code == KeyCode.DELETE){
                    Main.workingWindow.arrangementWindow.selectionBlock.del()
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
            if(event.code == KeyCode.C){
                workingWindow.arrangementWindow.timePointerCentering = false
            }
        }

        stage.onCloseRequest = EventHandler<WindowEvent> {
            Platform.exit()
        }

        renderManager.renderStart(imageView)
        object : AnimationTimer() {
            override fun handle(now: Long) {
                renderManager.perFrame()
                workingWindow.arrangementWindow.perFrame()
            }
        }.start()


        //Test audio
        audio.load(File("test.aiff"))
    }

    companion object {
        var renderManager = RenderManager()
        val workingWindow = WorkingWindow()
        val audio = Audio()

        var internalObject: Any = Any()

        @JvmStatic fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}
