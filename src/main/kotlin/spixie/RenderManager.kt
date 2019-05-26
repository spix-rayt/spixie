package spixie

import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.apache.commons.collections4.map.ReferenceMap
import org.apache.commons.lang3.time.StopWatch
import spixie.static.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class RenderManager {
    val bpm = NumberControl(160.0, "BPM")

    val time = TimeProperty(bpm)

    val offset = NumberControl(0.0, "Offset").apply {
        changes.subscribe {
            Core.arrangementWindow.spectrogram.requestRedraw()
        }
    }

    private val frameCache = ReferenceMap<Int, ByteArray>()

    private val forceRender = BehaviorSubject.createDefault(Unit).toSerialized()

    @Volatile var autoRenderNextFrame = false

    val lastRenderInfoSubject = BehaviorSubject.createDefault("0 particles - 0 ms").toSerialized()

    var scaleDown:Int = 2
        set(value) {
            field = value
            requestRender()
        }

    init {
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

    private val renderThread = Schedulers.newThread()

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

        Observable.combineLatest(time.timeChanges.distinctUntilChanged(), forceRender, BiFunction { t:Double, _:Unit -> t })
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(renderThread, false, 1)
                .subscribe { t ->
                    try {
                        val frame = Math.round(t*3600/bpm.value).toInt()
                        if (!Core.audio.isPlaying()) {
                            val stopWatch = StopWatch.createStarted()
                            val image = Core.arrangementWindow.visualEditor.render(t, scaleDown)
                            val bufferedImage = image.toBufferedImageAndRelease()
                            stopWatch.stop()
                            lastRenderInfoSubject.onNext("${image.particlesCount.toString().padStart(8, ' ')} particles ${stopWatch.getTime(TimeUnit.MILLISECONDS).toString().padStart(8, ' ')} ms")
                            images.onNext(SwingFXUtils.toFXImage(bufferedImage, null))
                            Flowable.fromCallable { bufferedImage.toJPEGByteArray() }
                                    .subscribeOn(Schedulers.computation())
                                    .observeOn(renderThread)
                                    .subscribe {
                                        frameCache[frame] = it
                                    }

                            if(autoRenderNextFrame){
                                Platform.runLater {
                                    time.frame = time.frame/3*3+3
                                }
                            }
                        }
                    } catch (e: ConcurrentModificationException) {
                        requestRender()
                        println("ConcurrentModificationException")
                    } catch (e: StackOverflowError) {
                        println("Stack overflow")
                    }
                }
    }

    fun requestRender(){
        forceRender.onNext(Unit)
    }

    fun renderToFile(frameRenderedToFileEventHandler: (currentFrame: Int, framesCount: Int) -> Unit, renderToFileCompleted: () -> Unit, motionBlurIterations: Int, startFrame: Int, endFrame: Int, audio: Boolean, offsetAudio: Double, lowQuality: Boolean) {
        Thread(Runnable {
            val downscale = if(lowQuality) 2 else 1
            val fpsskip = if(lowQuality) 3 else 1
            val w = 1920 / downscale
            val h = 1080 / downscale
            val fps = 60 / fpsskip
            val countFrames = endFrame-startFrame+1
            val ifAudio = { v: String-> if(audio) v else null }
            val processBuilder = ProcessBuilder(
                    listOfNotNull(
                            Settings.ffmpeg,
                            "-y",
                            "-f", "image2pipe",
                            "-framerate", "$fps",
                            "-i", "-",
                            ifAudio("-itsoffset"), ifAudio(offsetAudio.toString()),
                            ifAudio("-i"), ifAudio("audio.aiff"),
                            "-vf", "scale=$w:$h",
                            "-ss", "${startFrame/fps.toDouble()}",
                            "-c:v", "libx264",
                            "-tune", "animation",
                            "-x264-params","keyint=10",
                            "-crf", if(lowQuality) "23" else "17",
                            "-pix_fmt", "yuv420p",
                            if(lowQuality) "-filter" else null, if(lowQuality) "minterpolate='fps=60'" else null,
                            ifAudio("-c:a"), ifAudio("aac"),
                            "-shortest",
                            "out.mp4"
                    )
            )
            val process = processBuilder.start()
            val outputStream = process.outputStream
            val inputStream = process.inputStream
            val errorStream = process.errorStream

            try{
                kotlin.run {
                    val bufferedImage1 = BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)
                    for(frame in 0 until startFrame step fpsskip){
                        ImageIO.write(bufferedImage1, "png", outputStream)
                    }
                }

                for (frame in startFrame..endFrame step fpsskip) {
                    if(!process.isAlive){
                        break
                    }
                    val bufferedImage = Flowable.fromArray(*((0 until motionBlurIterations).toList().toTypedArray()))
                            .subscribeOn(renderThread)
                            .map { k->
                                Core.arrangementWindow.visualEditor.render(
                                        linearInterpolate(
                                                frameToTime(frame - fpsskip, bpm.value),
                                                frameToTime(frame, bpm.value),
                                                (k + 1) / motionBlurIterations.toDouble()
                                        ),
                                        downscale
                                ).buffer
                            }
                            .map { buffer->
                                Core.opencl.brightPixelsToWhite(buffer, w, h).also { buffer.release() }
                            }
                            .reduce(Core.opencl.createZeroBuffer(w*h*3)) { accum, pixelsBuffer->
                                Core.opencl.pixelSum(accum, pixelsBuffer, w, h, 1.0f/motionBlurIterations)
                                pixelsBuffer.release()
                                accum
                            }
                            .map { result->
                                result.toBufferedImage(w, h)
                            }.blockingGet()

                    ImageIO.write(bufferedImage, "png", outputStream)
                    inputStream.printAvailable()
                    errorStream.printAvailable()

                    Platform.runLater { frameRenderedToFileEventHandler(frame-startFrame+1, countFrames) }
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
            try{
                inputStream.printAvailable()
                errorStream.printAvailable()
                outputStream.close()
                process.waitFor()
                println("Process finished with exit code ${process.exitValue()}")
            }catch (e:Exception){
                e.printStackTrace()
            }

            Platform.runLater { renderToFileCompleted() }
        }).start()
    }

    fun clearCache(){
        Completable.fromAction {
            frameCache.clear()
            requestRender()
        }.subscribeOn(renderThread).subscribe()
    }
}
