package spixie

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.TreeItem
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.stage.WindowEvent
import jfxtras.scene.control.window.Window
import spixie.components.ParticleSpray
import spixie.dialogs.DialogManager

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

        val scene = Scene(root)
        scene.stylesheets.add("style.css")
        imageView.fitWidthProperty().bind(scene.widthProperty())
        imageView.fitHeightProperty().bind(scene.heightProperty())
        stage.title = "Render"
        stage.scene = scene
        stage.isMaximized = true
        stage.fullScreenExitKeyCombination = KeyCombination.NO_MATCH
        stage.show()
        stage.isFullScreen = true
        mainStage = stage

        val window = Window("")
        window.contentPane.children.addAll(controllerStage)
        window.setPrefSize(600.0, 600.0)
        window.setMinSize(300.0, 300.0)
        controllerStage.prefWidthProperty().bind(window.contentPane.widthProperty())
        controllerStage.prefHeightProperty().bind(window.contentPane.heightProperty())
        window.isResizableWindow = true
        window.isMovable = true
        window.resizableBorderWidth = 4.0
        root.children.addAll(window)

        val windowOpacity = arrayOf(0.6f)
        window.style = "-fx-opacity: " + windowOpacity[0]

        root.onKeyPressed = EventHandler<KeyEvent> { event ->
            if (event.code == KeyCode.DIGIT1 && event.isControlDown) windowOpacity[0] = 1.0f
            if (event.code == KeyCode.DIGIT2 && event.isControlDown) windowOpacity[0] = 0.8f
            if (event.code == KeyCode.DIGIT3 && event.isControlDown) windowOpacity[0] = 0.6f
            if (event.code == KeyCode.DIGIT4 && event.isControlDown) windowOpacity[0] = 0.4f
            if (event.code == KeyCode.DIGIT5 && event.isControlDown) windowOpacity[0] = 0.2f
            if (event.code == KeyCode.DIGIT6 && event.isControlDown) windowOpacity[0] = 0.0f
            if (event.code == KeyCode.H) {
                window.style = "-fx-opacity: 0.0"
            } else {
                window.style = "-fx-opacity: " + windowOpacity[0]
            }

            if(event.code == KeyCode.A){
                val frame = world.frame.get()
                if(frame>0) world.frame.set(frame-1)
            }
            if(event.code == KeyCode.D){
                val frame = world.frame.get()
                world.frame.set(frame+1)
            }
            if(event.code == KeyCode.P){
                world.autoRenderNextFrame = true
            }
        }

        root.onKeyReleased = EventHandler<KeyEvent> { event ->
            if (event.code == KeyCode.H) {
                window.style = "-fx-opacity: " + windowOpacity[0]
            }
            if(event.code == KeyCode.P){
                world.autoRenderNextFrame = false
            }
        }

        stage.onCloseRequest = EventHandler<WindowEvent> {
            Main.world.currentRenderThread.interrupt()
            Platform.exit()
        }

        loadTestData()

        world.renderStart(imageView)
    }

    fun loadTestData(){
        val component = ParticleSpray()
        val props = component.createPropsPane()
        world.root.children.addAll(
                TreeItem<ComponentsListItem>(
                        ComponentObject(component, props)
                )
        )
    }

    companion object {
        var dialogManager:DialogManager? = null
        var mainStage:Stage? = null
        var world: World = World()
        val controllerStage = ControllerStage()

        @JvmStatic fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}
