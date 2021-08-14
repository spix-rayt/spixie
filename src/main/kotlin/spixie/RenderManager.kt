package spixie

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.*
import org.apache.commons.lang3.time.StopWatch
import spixie.static.*
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class RenderManager {
    @Volatile var autoRenderNextFrame = false

    val renderCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    var needRender = true

    var scaleDown:Int = 2
        set(value) {
            field = value
            requestRender()
        }

    private val frameCache = FrameCache()

    init {
        projectWindow.bpm.apply {
            changes.subscribe { _->
                timelineWindow.spectrogram.requestRedraw()
            }

            Observable.zip(changes, changes.skip(1)) { previous: Double, current: Double -> previous to current }
                .subscribe { pair ->
                val t = projectWindow.timeHolder.beats - projectWindow.offset.value
                projectWindow.offset.value -= t*(pair.second/pair.first)-t
            }
        }
        renderStart()
    }

    fun doEveryFrame() {
        if(audio.isPlaying()) {
            val seconds = audio.getTime()
            projectWindow.timeHolder.seconds = seconds
            Platform.runLater {
                frameCache.getImageOrNUll(projectWindow.timeHolder.beats)?.let {
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
                            visualEditor.render(imageCLBuffer, projectWindow.timeHolder.beats, projectWindow.samplesPerPixel.value.roundToInt())
                            val bufferedImage = openCLApi.imageCLBufferToBufferedImage(imageCLBuffer)
                            stopWatch.stop()
                            //lastRenderInfoSubject.onNext("${imageFloatBuffer.particlesCount.toString().padStart(8, ' ')} particles ${stopWatch.getTime(TimeUnit.MILLISECONDS).toString().padStart(8, ' ')} ms")
                            val renderInfo = "${stopWatch.getTime(TimeUnit.MILLISECONDS).toString().padStart(8, ' ')} ms"
                            Platform.runLater {
                                projectWindow.lastRenderInfo.text = renderInfo
                                FXApplication.instance?.updateImage(SwingFXUtils.toFXImage(bufferedImage, null))
                            }

                            launch {
                                frameCache.putImage(bufferedImage, projectWindow.timeHolder.beats)
                            }

                            if(autoRenderNextFrame) {
                                Platform.runLater {
                                    projectWindow.timeHolder.frame = projectWindow.timeHolder.frame + 1
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

        projectWindow.timeHolder.timeChanges.distinctUntilChanged().subscribe {
            requestRender()
        }
    }

    fun requestRender() = GlobalScope.launch(renderCoroutineDispatcher) {
        needRender = true
    }

    fun renderToFile(frameRenderedToFileEventHandler: (currentFrame: Int, framesCount: Int) -> Unit, renderToFileCompleted: () -> Unit, motionBlurIterations: Int, beatStart: Double, beatEnd: Double, audio: Boolean, offsetAudio: Double, fps: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val selectedFrames = IntRange(
                beatsToFrames(beatStart, projectWindow.bpm.value, fps),
                beatsToFrames(beatEnd, projectWindow.bpm.value, fps)
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
                                frameToBeats(frame - 1, projectWindow.bpm.value, fps),
                                frameToBeats(frame, projectWindow.bpm.value, fps),
                                (k + 1) / motionBlurIterations.toDouble()
                            )
                            visualEditor.render(
                                imageFloatBuffer,
                                t,
                                projectWindow.samplesPerPixel.value.roundToInt()
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
