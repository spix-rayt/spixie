package spixie

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import javafx.stage.WindowEvent
import jfxtras.scene.control.window.Window

class Main : Application() {
    @Throws(Exception::class)
    override fun start(stage: Stage) {
        val root = Group()
        val imageView = ImageView()
        imageView.isSmooth = true
        imageView.isPreserveRatio = false
        root.children.addAll(imageView)

        val scene = Scene(root)
        scene.stylesheets.add("style.css")
        imageView.fitWidthProperty().bind(scene.widthProperty())
        imageView.fitHeightProperty().bind(scene.heightProperty())
        stage.title = "Render"
        stage.scene = scene
        stage.isMaximized = true
        stage.show()
        world.renderStart(imageView)

        val window = Window("")
        window.contentPane.children.addAll(controllerStage)
        window.setPrefSize(800.0, 800.0)
        window.setMinSize(400.0, 500.0)
        controllerStage.prefWidthProperty().bind(window.contentPane.widthProperty())
        controllerStage.prefHeightProperty().bind(window.contentPane.heightProperty())
        window.isResizableWindow = true
        window.isMovable = true
        window.resizableBorderWidth = 4.0
        root.children.addAll(window)

        val windowOpacity = arrayOf(1.0f)

        root.onKeyPressed = EventHandler<KeyEvent> { event ->
            if (event.code == KeyCode.DIGIT1 && event.isControlDown) windowOpacity[0] = 1.0f
            if (event.code == KeyCode.DIGIT2 && event.isControlDown) windowOpacity[0] = 0.8f
            if (event.code == KeyCode.DIGIT3 && event.isControlDown) windowOpacity[0] = 0.6f
            if (event.code == KeyCode.DIGIT4 && event.isControlDown) windowOpacity[0] = 0.4f
            if (event.code == KeyCode.DIGIT5 && event.isControlDown) windowOpacity[0] = 0.2f
            if (event.code == KeyCode.DIGIT6 && event.isControlDown) windowOpacity[0] = 0.0f
            if (event.code == KeyCode.TAB) {
                window.style = "-fx-opacity: 0.0"
            } else {
                window.style = "-fx-opacity: " + windowOpacity[0]
            }
        }

        root.onKeyReleased = EventHandler<KeyEvent> { event ->
            if (event.code == KeyCode.TAB) {
                window.style = "-fx-opacity: " + windowOpacity[0]
            }
        }

        stage.onCloseRequest = EventHandler<WindowEvent> {
            Main.world.currentRenderThread.interrupt()
            Platform.exit()
        }
    }

    companion object {
        var world: World = World()
        val controllerStage = ControllerStage()

        @JvmStatic fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}
