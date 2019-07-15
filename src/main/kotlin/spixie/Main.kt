package spixie

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.scene.CacheHint
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.util.Duration
import spixie.opencl.OpenCLInfoWindow
import java.io.File
import java.io.ObjectInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class Main : Application() {
    @Throws(Exception::class)
    override fun start(stage: Stage) {
        val root = StackPane()
        val imageView = ImageView().apply {
            style="-fx-background:transparent;"
            isSmooth = true
            isPreserveRatio = false
            isCache = false
            cacheHint = CacheHint.SPEED
        }
        val images = PublishSubject.create<Image>().toSerialized()
        images.observeOn(JavaFxScheduler.platform()).subscribe {
            imageView.image = it
            if(it.width*2 > root.width){
                imageView.scaleX = 1.0
                imageView.scaleY = 1.0
            }else{
                imageView.scaleX = 2.0
                imageView.scaleY = 2.0
            }
        }
        root.children.addAll(imageView)
        root.style = "-fx-background-color: #111111;"

        val scene = Scene(root, 100.0, 100.0)
        scene.stylesheets.add("style.css")
        scene.focusOwnerProperty().addListener { _, _, newValue ->
            if(newValue == null){
                Core.workingWindow.center.requestFocus()
            }
        }



        stage.apply {
            title = "Render"
            this.scene = scene
            isMaximized = true
            fullScreenExitKeyCombination = KeyCombination.NO_MATCH
            show()
            isFullScreen = true
        }

        Core.workingWindow.prefWidthProperty().bind(scene.widthProperty())
        Core.workingWindow.prefHeightProperty().bind(scene.heightProperty())
        root.children.addAll(Core.workingWindow)
        Core.workingWindow.open(Core.arrangementWindow)
        val windowOpacity = BehaviorSubject.createDefault(1.0)
        val windowHide = BehaviorSubject.createDefault(false)
        Observables.combineLatest(windowOpacity, windowHide) { opacity, hide ->
            Core.workingWindow.opacity = if(hide) 0.0 else opacity
        }.subscribe()

        var playStartTime = 0.0

        root.onKeyPressed = EventHandler<KeyEvent> { event ->
            if(event.isControlDown && !event.isAltDown && !event.isShiftDown){
                if (event.code == KeyCode.DIGIT1 && event.isControlDown) {
                    windowOpacity.onNext(1.0)
                }
                if (event.code == KeyCode.DIGIT2 && event.isControlDown) {
                    windowOpacity.onNext(0.8)
                }
                if (event.code == KeyCode.DIGIT3 && event.isControlDown) {
                    windowOpacity.onNext(0.6)
                }
                if (event.code == KeyCode.DIGIT4 && event.isControlDown) {
                    windowOpacity.onNext(0.4)
                }
                if (event.code == KeyCode.DIGIT5 && event.isControlDown) {
                    windowOpacity.onNext(0.2)
                }
                if (event.code == KeyCode.DIGIT6 && event.isControlDown) {
                    windowOpacity.onNext(0.0)
                }
            }

            if(!event.isControlDown && !event.isAltDown && !event.isShiftDown){
                if (event.code == KeyCode.TAB) {
                    windowHide.onNext(true)
                }

                if(event.code == KeyCode.A){
                    Core.renderManager.time.frame = (Core.renderManager.time.frame-1).coerceAtLeast(0)
                }
                if(event.code == KeyCode.D){
                    Core.renderManager.time.frame += 1
                }
                if(event.code == KeyCode.P){
                    if(!Core.renderManager.autoRenderNextFrame){
                        Core.renderManager.autoRenderNextFrame = true
                        Core.renderManager.time.frame = Core.renderManager.time.frame/3*3
                        Core.renderManager.requestRender()
                    }
                }
                if(event.code == KeyCode.SPACE){
                    if(Core.audio.isPlaying()){
                        Core.audio.pause()
                        Platform.runLater { Core.renderManager.time.time = playStartTime }
                    }else{
                        playStartTime = Core.renderManager.time.time
                        Core.audio.play(Duration.seconds(Math.round((playStartTime-Core.renderManager.offset.value)*3600/Core.renderManager.bpm.value)/60.0))
                    }
                }
                if(event.code == KeyCode.F2){
                    OpenCLInfoWindow(scene.window)
                }
                if(event.code == KeyCode.F8){
                    if(!File("screenshots/").exists()) File("screenshots/").mkdir()
                    ImageIO.write(SwingFXUtils.fromFXImage(imageView.image, null), "png", File("screenshots/${SimpleDateFormat("yyyy-MM-dd_HHmmss").format(Calendar.getInstance().time)}.png"))
                }
            }
        }

        root.onKeyReleased = EventHandler<KeyEvent> { event ->
            if (event.code == KeyCode.TAB) {
                windowHide.onNext(false)
            }
            if(event.code == KeyCode.P){
                Core.renderManager.autoRenderNextFrame = false
            }
        }

        stage.onCloseRequest = EventHandler<WindowEvent> {
            val bytes = Core.arrangementWindow.serialize()
            if(!File("save/").exists()) File("save/").mkdir()
            if(File("save/save.spixie").exists()){
                Files.move(Paths.get("save/save.spixie"), Paths.get("save/save${SimpleDateFormat("yyyy-MM-dd_HHmmss").format(Calendar.getInstance().time)}.spixie"), StandardCopyOption.REPLACE_EXISTING)
            }
            Files.write(Paths.get("save/save.spixie"), bytes)
            Platform.exit()
        }

        File("save/save.spixie").let {
            if(it.exists()){
                Core.arrangementWindow.deserializeAndLoad(ObjectInputStream(it.inputStream()))
            }
        }

        Core.renderManager.renderStart(images)
        object : AnimationTimer() {
            override fun handle(now: Long) {
                Core.renderManager.perFrame()
                if(Core.workingWindow.opacity != 0.0){
                    Core.arrangementWindow.perFrame()
                }
            }
        }.start()

        /*if(File("audio.aiff").exists()) {
            Core.audio.load(File("audio.aiff"))
        }*/
    }
}

fun main() {
    Locale.setDefault(Locale.ENGLISH)
    Application.launch(Main::class.java)
}
