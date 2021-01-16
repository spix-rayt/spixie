package spixie

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.*
import org.apache.commons.collections4.map.ReferenceMap
import org.apache.commons.lang3.time.StopWatch
import spixie.raymarching.RayMarchingRenderer
import spixie.raymarching.geometryobject.Sphere
import spixie.static.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.roundToInt

@ObsoleteCoroutinesApi
class RenderManager {
    val bpm = NumberControl(160.0, "BPM")

    val time = TimeProperty(bpm)

    val offset = NumberControl(0.0, "Offset").apply {
        changes.subscribe {
            Core.arrangementWindow.spectrogram.requestRedraw()
        }
    }

    private val frameCache = ReferenceMap<Int, ByteArray>()

    @Volatile var autoRenderNextFrame = false

    val lastRenderInfoSubject = BehaviorSubject.createDefault("0 particles - 0 ms").toSerialized()

    val renderCoroutineDispatcher = newSingleThreadContext("render-thread")

    var needRender = true

    var scaleDown:Int = 2
        set(value) {
            field = value
            GlobalScope.launch {
                Core.renderManager.requestRender()
            }
        }

    init {
        Gamepad.start()
        bpm.apply {
            limitMin(60.0)
            limitMax(999.0)
            changes.subscribe { _->
                Core.arrangementWindow.spectrogram.requestRedraw()
            }

            Observable.zip(changes, changes.skip(1), BiFunction { previous: Double, current: Double -> previous to current }).subscribe { pair ->
                val t = time.time - offset.value
                offset.value -= t*(pair.second/pair.first)-t
            }
        }
    }

    var perFrame = {  }

    fun renderStart(images: Subject<Image>) {
        perFrame = {
            if(Core.audio.isPlaying()){
                val seconds = Core.audio.getTime()
                time.time = (frameToTime((seconds*60).roundToInt(), Core.renderManager.bpm.value) + offset.value).coerceAtLeast(0.0)
                frameCache[time.frame/3*3]?.let {
                    val byteArrayInputStream = ByteArrayInputStream(it)
                    images.onNext(Image(byteArrayInputStream))
                }
            }
        }

        GlobalScope.launch(renderCoroutineDispatcher) {
            while(true) {
                if(needRender) {
                    try {
                        val frame = Math.round(time.time * 3600.0 / bpm.value).toInt()
                        if (!Core.audio.isPlaying()) {
                            val stopWatch = StopWatch.createStarted()
                            //val image = Core.arrangementWindow.visualEditor.render(t, scaleDown)
                            val objects = arrayListOf(
                                    Sphere().apply {
                                        x = 0.0f
                                        y = 0.0f
                                        z = -10.0f
                                        radius = 1.0f
                                    }
                            )
                            val image = RayMarchingRenderer.render(objects, scaleDown)
                            val bufferedImage = image.toBufferedImageAndRelease()
                            stopWatch.stop()
                            lastRenderInfoSubject.onNext("${image.particlesCount.toString().padStart(8, ' ')} particles ${stopWatch.getTime(TimeUnit.MILLISECONDS).toString().padStart(8, ' ')} ms")
                            images.onNext(SwingFXUtils.toFXImage(bufferedImage, null))

                            launch {
                                frameCache[frame] = bufferedImage.toJPEGByteArray(0.7f)
                            }

                            if(autoRenderNextFrame){
                                Platform.runLater {
                                    time.frame = time.frame/3*3+3
                                }
                            }
                        }
                    } catch (e: ConcurrentModificationException) {
                        e.printStackTrace()
                    } catch (e: StackOverflowError) {
                        e.printStackTrace()
                    }

                    needRender = false
                }
                delay(1)
            }
        }

        time.timeChanges.distinctUntilChanged().subscribe {
            GlobalScope.launch {
                Core.renderManager.requestRender()
            }
        }
    }

    suspend fun requestRender() = withContext(renderCoroutineDispatcher) {
        needRender = true
    }

    fun renderToFile(frameRenderedToFileEventHandler: (currentFrame: Int, framesCount: Int) -> Unit, renderToFileCompleted: () -> Unit, motionBlurIterations: Int, startFrame: Int, endFrame: Int, audio: Boolean, offsetAudio: Double, lowQuality: Boolean) {
        thread {
            val downscale = if (lowQuality) 2 else 1
            val fpsskip = if (lowQuality) 3 else 1
            val w = 1920 / downscale
            val h = 1080 / downscale
            val fps = 60 / fpsskip
            val countFrames = endFrame - startFrame + 1
            val ifAudio = { v: String -> if (audio) v else null }
            val processBuilder = ProcessBuilder(
                listOfNotNull(
                    Settings.ffmpeg,
                    "-y",
                    "-f", "image2pipe",
                    "-framerate", "$fps",
                    "-i", "-",
                    ifAudio("-itsoffset"), ifAudio(offsetAudio.toString()),
                    ifAudio("-i"), ifAudio("audio.aiff"),
                    "-ss", "${startFrame / fps.toDouble()}",
                    "-c:v", "h264",
                    "-g", "1",
                    ifAudio("-c:a"), ifAudio("aac"),
                    "-shortest",
                    "out.avi"
                )
            )
            val process = processBuilder.start()
            val outputStream = process.outputStream
            val inputStream = process.inputStream
            val errorStream = process.errorStream

            try {
                run {
                    val bufferedImage1 = BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)
                    for (frame in 0 until startFrame step fpsskip) {
                        ImageIO.write(bufferedImage1, "png", outputStream)
                    }
                }

                for (frame in startFrame..endFrame step fpsskip) {
                    if (!process.isAlive) {
                        break
                    }
                    val bufferedImage = runBlocking(renderCoroutineDispatcher) {
                        (0 until motionBlurIterations)
                            .map { k ->
                                Core.arrangementWindow.visualEditor.render(
                                    linearInterpolate(
                                        frameToTime(frame - fpsskip, bpm.value),
                                        frameToTime(frame, bpm.value),
                                        (k + 1) / motionBlurIterations.toDouble()
                                    ),
                                    downscale
                                ).buffer
                            }
                            .map { buffer ->
                                Core.opencl.brightPixelsToWhite(buffer, w, h).also { buffer.release() }
                            }
                            .reduce { accum, pixelsBuffer ->
                                Core.opencl.pixelSum(accum, pixelsBuffer, w, h, 1.0f / motionBlurIterations)
                                pixelsBuffer.release()
                                accum
                            }.toBufferedImage(w, h)
                    }

                    ImageIO.write(bufferedImage, "png", outputStream)
                    inputStream.printAvailable()
                    errorStream.printAvailable()

                    Platform.runLater { frameRenderedToFileEventHandler(frame - startFrame + 1, countFrames) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                inputStream.printAvailable()
                errorStream.printAvailable()
                outputStream.close()
                process.waitFor()
                println("Process finished with exit code ${process.exitValue()}")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Platform.runLater { renderToFileCompleted() }
        }
    }

    fun clearCache() {
        GlobalScope.launch(renderCoroutineDispatcher) {
            frameCache.clear()
            requestRender()
        }
    }
}
