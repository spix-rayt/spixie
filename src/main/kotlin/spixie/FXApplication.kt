package spixie

import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
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
import javafx.scene.layout.StackPane
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import spixie.render.OpenCLInfoWindow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.roundToLong

class FXApplication : Application() {
    lateinit var firstStage: Stage
    var secondStage: Stage? = null

    private val imageView = ImageView().apply {
        style="-fx-background:transparent;"
        isSmooth = true
        isPreserveRatio = true
        isCache = false
        cacheHint = CacheHint.SPEED
    }

    private val firstStageRoot = StackPane().apply {
        children.addAll(imageView)
        style = "-fx-background-color: #111111;"
    }

    private val secondStageRoot = StackPane().apply {
        style = "-fx-background-color: #111111;"
    }

    fun updateImage(image: Image) {
        imageView.image = image
        if(image.width.toInt() * 2 >= firstStageRoot.width.toInt()) {
            imageView.fitWidth = firstStageRoot.width
            imageView.fitHeight = firstStageRoot.height
        } else {
            imageView.fitWidth = image.width * 2.0
            imageView.fitHeight = image.height * 2.0
        }
    }

    override fun start(stage: Stage) {
        instance = this
        firstStage = stage
        firstStage.scene = Scene(firstStageRoot, 100.0, 100.0)
        firstStage.scene.stylesheets.add("style.css")

        firstStage.apply {
            isMaximized = true
            firstStage.initStyle(StageStyle.UNDECORATED)
            show()
        }

        setWindowsMode(WindowsMode.SINGLE)

        workWindow.open(timelineWindow)
        val windowOpacity = BehaviorSubject.createDefault(0.0)
        val windowHide = BehaviorSubject.createDefault(false)
        Observables.combineLatest(windowOpacity, windowHide) { opacity, hide ->
            workWindow.opacity = if(hide) 0.0 else opacity
        }.subscribe()

        var playStartTime = 0.0

        firstStageRoot.onKeyPressed = EventHandler { event ->
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
                    renderManager.timeHolder.frame = (renderManager.timeHolder.frame-1).coerceAtLeast(0)
                }
                if(event.code == KeyCode.D){
                    renderManager.timeHolder.frame += 1
                }
                if(event.code == KeyCode.P){
                    if(!renderManager.autoRenderNextFrame) {
                        renderManager.autoRenderNextFrame = true
                        renderManager.timeHolder.frame = renderManager.timeHolder.frame
                        renderManager.requestRender()
                    }
                }
                if(event.code == KeyCode.SPACE){
                    if(audio.isPlaying()){
                        audio.pause()
                        Platform.runLater { renderManager.timeHolder.beats = playStartTime }
                    }else{
                        playStartTime = renderManager.timeHolder.beats
                        audio.play(Duration.seconds(((playStartTime - renderManager.offset.value) * 3600 / renderManager.bpm.value).roundToLong() / 60.0))
                    }
                }
                if(event.code == KeyCode.F2){
                    OpenCLInfoWindow(firstStage.scene.window)
                }
                if(event.code == KeyCode.F8){
                    if(!File("screenshots/").exists()) File("screenshots/").mkdir()
                    ImageIO.write(SwingFXUtils.fromFXImage(imageView.image, null), "png", File("screenshots/${SimpleDateFormat("yyyy-MM-dd_HHmmss").format(Calendar.getInstance().time)}.png"))
                }
            }
        }

        firstStageRoot.onKeyReleased = EventHandler { event ->
            if (event.code == KeyCode.TAB) {
                windowHide.onNext(false)
            }
            if(event.code == KeyCode.P){
                renderManager.autoRenderNextFrame = false
            }
        }

        firstStage.onCloseRequest = EventHandler {
//            val bytes = arrangementWindow.serialize()
//            if(!File("save/").exists()) File("save/").mkdir()
//            if(File("save/save.spixie").exists()){
//                Files.move(Paths.get("save/save.spixie"), Paths.get("save/save${SimpleDateFormat("yyyy-MM-dd_HHmmss").format(Calendar.getInstance().time)}.spixie"), StandardCopyOption.REPLACE_EXISTING)
//            }
//            Files.write(Paths.get("save/save.spixie"), bytes)
            Platform.exit()
        }

//        File("save/save.spixie").let {
//            if(it.exists()){
//                arrangementWindow.deserializeAndLoad(it.inputStream())
//            }
//        }

        object : AnimationTimer() {
            override fun handle(now: Long) {
                renderManager.doEveryFrame()
                if(workWindow.opacity != 0.0){
                    timelineWindow.doEveryFrame()
                }
            }
        }.start()

        /*if(File("audio.aiff").exists()) {
            audio.load(File("audio.aiff"))
        }*/
    }

    fun setWindowsMode(windowsMode: WindowsMode) {
        when(windowsMode) {
            WindowsMode.SINGLE -> {
                workWindow.prefWidthProperty().bind(firstStage.scene.widthProperty())
                workWindow.prefHeightProperty().bind(firstStage.scene.heightProperty())
                firstStageRoot.children.addAll(workWindow)

                firstStage.scene.focusOwnerProperty().addListener { _, _, newValue ->
                    if(newValue == null){
                        workWindow.center.requestFocus()
                    }
                }

                secondStage?.close()
            }
            WindowsMode.MULTIPLE -> {
                if(secondStage == null) {
                    val stage = Stage()
                    secondStage = stage
                    firstStageRoot.children.remove(workWindow)

                    stage.scene = Scene(secondStageRoot, 100.0, 100.0)
                    stage.scene.stylesheets.add("style.css")
                    stage.scene.focusOwnerProperty().addListener { _, _, newValue ->
                        if(newValue == null) {
                            workWindow.center.requestFocus()
                        }
                    }

                    workWindow.prefWidthProperty().bind(stage.scene.widthProperty())
                    workWindow.prefHeightProperty().bind(stage.scene.heightProperty())
                    secondStageRoot.children.addAll(workWindow)

                    val screen = Screen.getScreens().getOrNull(1)
                    if(screen != null) {
                        stage.x = screen.visualBounds.minX
                        stage.y = screen.visualBounds.minY
                    }
                    stage.apply {
                        initOwner(firstStage)
                        initStyle(StageStyle.UNDECORATED)
                        isMaximized = true
                        show()
                    }
                }
            }
        }
    }

    enum class WindowsMode {
        SINGLE,
        MULTIPLE
    }

    companion object {
        @Volatile
        var instance: FXApplication? = null
    }
}