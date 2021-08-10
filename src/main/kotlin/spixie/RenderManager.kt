package spixie

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.*
import org.apache.commons.lang3.time.StopWatch
import spixie.static.*
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class RenderManager {
    val bpm = NumberControl(160.0, "BPM", 0.0).apply {
        minNumberLineScale = 0.0
        maxNumberLineScale = 0.0
        limitMin(60.0)
        limitMax(999.0)
    }

    val fps = NumberControl(24.0, "FPS")

    val offset = NumberControl(0.0, "Offset").apply {
        changes.subscribe {
            timelineWindow.spectrogram.requestRedraw()
        }
    }

    val timeHolder = TimeHolder(bpm, fps, offset)

    val samplesPerPixel = NumberControl(20.0, "SPP", 0.0).apply {
        limitMin(1.0)
        limitMax(5000.0)
        minNumberLineScale = 0.0
        maxNumberLineScale = 0.0
        changes.subscribe {
            requestRender()
        }
    }

    @Volatile var autoRenderNextFrame = false

    val lastRenderInfoSubject = BehaviorSubject.createDefault("0 particles - 0 ms").toSerialized()

    val renderCoroutineDispatcher = newSingleThreadContext("render-thread")

    var needRender = true

    var scaleDown:Int = 2
        set(value) {
            field = value
            requestRender()
        }

    private val frameCache = FrameCache()

    init {
        bpm.apply {
            changes.subscribe { _->
                timelineWindow.spectrogram.requestRedraw()
            }

            Observable.zip(changes, changes.skip(1)) { previous: Double, current: Double -> previous to current }
                .subscribe { pair ->
                val t = timeHolder.beats - offset.value
                offset.value -= t*(pair.second/pair.first)-t
            }
        }
        renderStart()
    }

    fun doEveryFrame() {
        if(audio.isPlaying()) {
            val seconds = audio.getTime()
            timeHolder.seconds = seconds
            Platform.runLater {
                frameCache.getImageOrNUll(timeHolder.beats)?.let {
                    FXApplication.instance?.updateImage(it)
                }
            }
        }
    }

    private fun renderStart() {
        GlobalScope.launch(renderCoroutineDispatcher) {
            while(true) {
                if(needRender) {
                    try {
                        if (!audio.isPlaying()) {
                            val stopWatch = StopWatch.createStarted()
                            val imageCLBuffer = openCLApi.createImageCLBuffer((1920 / scaleDown).roundUp(2), (1080 / scaleDown).roundUp(2))
                            visualEditor.render(imageCLBuffer, timeHolder.beats, samplesPerPixel.value.roundToInt())
                            val bufferedImage = openCLApi.imageCLBufferToBufferedImage(imageCLBuffer)
                            stopWatch.stop()
                            //lastRenderInfoSubject.onNext("${imageFloatBuffer.particlesCount.toString().padStart(8, ' ')} particles ${stopWatch.getTime(TimeUnit.MILLISECONDS).toString().padStart(8, ' ')} ms")
                            lastRenderInfoSubject.onNext("${stopWatch.getTime(TimeUnit.MILLISECONDS).toString().padStart(8, ' ')} ms")
                            Platform.runLater {
                                FXApplication.instance?.updateImage(SwingFXUtils.toFXImage(bufferedImage, null))
                            }

                            launch {
                                frameCache.putImage(bufferedImage, timeHolder.beats)
                            }

                            if(autoRenderNextFrame){
                                Platform.runLater {
                                    timeHolder.frame = timeHolder.frame + 1
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

        timeHolder.timeChanges.distinctUntilChanged().subscribe {
            requestRender()
        }
    }

    fun requestRender() = GlobalScope.launch(renderCoroutineDispatcher) {
        needRender = true
    }

    fun renderToFile(frameRenderedToFileEventHandler: (currentFrame: Int, framesCount: Int) -> Unit, renderToFileCompleted: () -> Unit, motionBlurIterations: Int, beatStart: Double, beatEnd: Double, audio: Boolean, offsetAudio: Double, fps: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val selectedFrames = IntRange(
                beatsToFrames(beatStart, bpm.value, fps),
                beatsToFrames(beatEnd, bpm.value, fps)
            )

            val w = 1920
            val h = 1080
            val countFrames = selectedFrames.count()
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
                    "-ss", "${selectedFrames.start / fps.toDouble()}",
                    "-c:v", "h264",
                    "-g", "1",
                    ifAudio("-c:a"), ifAudio("aac"),
                    "-shortest",
                    "out.mkv"
                )
            )
            val process = processBuilder.start()
            val outputStream = process.outputStream
            val inputStream = process.inputStream
            val errorStream = process.errorStream

            try {
                run {
                    val bufferedImage1 = BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)
                    for (frame in 0 until selectedFrames.first) {
                        ImageIO.write(bufferedImage1, "png", outputStream)
                    }
                }

                for (frame in selectedFrames) {
                    if (!process.isAlive) {
                        break
                    }
                    val bufferedImage = runBlocking(renderCoroutineDispatcher) {
                        val imageFloatBuffer = openCLApi.createImageCLBuffer(w, h)
                        for(k in 0 until motionBlurIterations) {
                            val t = linearInterpolate(
                                frameToBeats(frame - 1, bpm.value, fps),
                                frameToBeats(frame, bpm.value, fps),
                                (k + 1) / motionBlurIterations.toDouble()
                            )
                            visualEditor.render(
                                imageFloatBuffer,
                                t,
                                samplesPerPixel.value.roundToInt()
                            )
                        }
                        val clBuffer = openCLApi.brightPixelsToWhite(imageFloatBuffer.buffer, w, h, 1.0f / motionBlurIterations).also { imageFloatBuffer.buffer.release() }
                        openCLApi.clBufferToBufferedImage(clBuffer, w, h)
                    }

                    ImageIO.write(bufferedImage, "png", outputStream)
                    inputStream.printAvailable()
                    errorStream.printAvailable()

                    Platform.runLater { frameRenderedToFileEventHandler(frame - selectedFrames.first + 1, countFrames) }
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
}
