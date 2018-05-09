package spixie

import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject
import javafx.animation.AnimationTimer
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.apache.commons.collections4.map.ReferenceMap
import spixie.static.*
import spixie.visual_editor.ParticleArray
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class RenderManager {
    val time = TimeProperty(160.0)
    var bpm = ValueControl(160.0, 1.0, "BPM")
    @Volatile var renderingToFile = false
    private val openCLRenderer:OpenCLRenderer = OpenCLRenderer()
    private val cache = ReferenceMap<Long, ByteArray>()
    private val frameCache = ReferenceMap<Double, ByteArray>()
    val forceRender = BehaviorSubject.createDefault(Unit).toSerialized()
    @Volatile var autoRenderNextFrame = false

    var scaleDown:Int = 2
        set(value) {
            field = value
            clearCache()
        }

    private fun resizeIfNotCorrect(width: Int, height: Int) {
        if (openCLRenderer.realWidth != width || openCLRenderer.realHeight != height) {
            openCLRenderer.setSize(width,height)
            clearCache()
        }
    }

    val renderThread = Schedulers.newThread()

    var perFrame = {  }

    fun renderStart(imageView: ImageView) {
        perFrame = {
            if(Main.audio.isPlaying()){
                val seconds = Main.audio.getTime()
                time.time = frameToTime((seconds*60).roundToInt(), Main.renderManager.bpm.value.value)
                frameCache[time.time]?.let {
                    val byteArrayInputStream = ByteArrayInputStream(it)
                    imageView.image = Image(byteArrayInputStream)
                }
            }
        }

        Observable.combineLatest(time.timeChanges.distinctUntilChanged(), forceRender, BiFunction { t:Double, _:Unit -> t })
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(renderThread, false, 1)
                .subscribe { t ->
                    try {
                        if (!renderingToFile && !Main.audio.isPlaying()) {
                            val particles = Main.arrangementWindow.visualEditor.render(t)
                            val imageCached = cache[particles.hash]
                            if(imageCached == null){
                                resizeIfNotCorrect(imageView.fitWidth.toInt()/scaleDown, imageView.fitHeight.toInt()/scaleDown)
                                val image = openclRender(particles).toPNGByteArray()
                                cache.put(particles.hash, image)
                                frameCache.put(t, image)
                            }else{
                                frameCache.put(t, imageCached)
                            }
                            runInUIAndWait {
                                frameCache[t]?.let {
                                    imageView.image = Image(ByteArrayInputStream(it))
                                }
                            }
                            if(autoRenderNextFrame){
                                runInUIAndWait {
                                    time.frame += 1
                                }
                            }
                        }
                    } catch (e: ConcurrentModificationException) {
                        forceRender.onNext(Unit)
                    }
                }
    }

    private fun openclRender(particleArray: ParticleArray):BufferedImage {
        val renderBufferBuilder = RenderBufferBuilder(particleArray.array.size)
        particleArray.array.forEach { particle ->
            renderBufferBuilder.addParticle(particle.x, particle.y, particle.size, particle.red, particle.green, particle.blue, particle.alpha)
        }
        return openCLRenderer.render(renderBufferBuilder.complete())
    }

    fun renderToFile(frameRenderedToFileEventHandler: FrameRenderedToFileEvent, renderToFileCompleted: RenderToFileCompleted) {
        renderingToFile = true
        Thread(Runnable {
            openCLRenderer.setSize(1920, 1080)
            val countFrames = time.frame + 1
            val processBuilder = ProcessBuilder(
                    listOf(
                            Settings.ffmpeg,
                            "-y",
                            "-f", "image2pipe",
                            "-framerate", "60",
                            "-i", "-",
                            "-i","test.aiff",
                            "-c:v", "libx264",
                            "-preset", "slow",
                            "-crf", "17",
                            "-pix_fmt", "yuv420p",
                            "-c:a","aac",
                            "-shortest",
                            "out.mp4"
                    )
            )
            val process = processBuilder.start()
            val outputStream = process.outputStream
            val inputStream = process.inputStream
            val errorStream = process.errorStream

            for (frame in 0 until countFrames) {
                if(!process.isAlive){
                    break
                }

                val bufferedImage = openclRender(Main.arrangementWindow.visualEditor.render(frameToTime(frame, bpm.value.value)))
                val bufferedImage1 = BufferedImage(bufferedImage.width, bufferedImage.height, BufferedImage.TYPE_3BYTE_BGR)
                bufferedImage1.graphics.drawImage(bufferedImage, 0, 0, null)
                ImageIO.write(bufferedImage1, "png", outputStream)

                inputStream.printAvailable()
                errorStream.printAvailable()

                Platform.runLater { frameRenderedToFileEventHandler.handle(frame + 1, countFrames) }
            }
            outputStream.close()
            process.waitFor()
            inputStream.printAvailable()
            errorStream.printAvailable()
            println("Process finished with exit code ${process.exitValue()}")
            renderingToFile = false
            Platform.runLater { renderToFileCompleted.handle() }
        }).start()
    }

    fun clearCache(){
        Completable.fromAction {
            cache.clear()
            frameCache.clear()
            forceRender.onNext(Unit)
        }.subscribeOn(renderThread).subscribe()
    }

    interface FrameRenderedToFileEvent {
        fun handle(currentFrame: Int, framesCount: Int)
    }

    interface RenderToFileCompleted {
        fun handle()
    }
}
