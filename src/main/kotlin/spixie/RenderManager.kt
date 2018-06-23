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
import spixie.static.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class RenderManager {
    val bpm = NumberControl(160.0, 1.0, "BPM")
    val time = TimeProperty(bpm)
    val offset = NumberControl(0.0, 0.01, "Offset").apply {
        changes.subscribe {
            Main.arrangementWindow.needRedrawWaveform = true
        }
    }
    private val frameCache = ReferenceMap<Int, ByteArray>()
    private val forceRender = BehaviorSubject.createDefault(Unit).toSerialized()
    @Volatile var autoRenderNextFrame = false
    val lastRenderedParticlesCount = BehaviorSubject.createDefault(0).toSerialized()

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
                Main.arrangementWindow.needRedrawWaveform = true
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
            if(Main.audio.isPlaying()){
                val seconds = Main.audio.getTime()
                time.time = (frameToTime((seconds*60).roundToInt(), Main.renderManager.bpm.value) + offset.value).coerceAtLeast(0.0)
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
                        if (!Main.audio.isPlaying()) {
                            val image = Main.arrangementWindow.visualEditor.render(t, scaleDown)
                            val bufferedImage = image.toBufferedImageAndRelease()
                            lastRenderedParticlesCount.onNext(image.particlesCount)
                            images.onNext(SwingFXUtils.toFXImage(bufferedImage, null))
                            Flowable.fromCallable { bufferedImage.toPNGByteArray() }
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

    fun renderToFile(frameRenderedToFileEventHandler: (currentFrame: Int, framesCount: Int) -> Unit, renderToFileCompleted: () -> Unit, motionBlurIterations: Int, startFrame: Int, endFrame: Int, audio: Boolean, offsetAudio: Double) {
        Thread(Runnable {
            val w = 1920
            val h = 1080
            val fps = 60
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
                            "-crf", "17",
                            "-pix_fmt", "yuv420p",
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
                    for(frame in 0 until startFrame){
                        ImageIO.write(bufferedImage1, "png", outputStream)
                    }
                }

                for (frame in startFrame..endFrame) {
                    if(!process.isAlive){
                        break
                    }
                    val bufferedImage = Flowable.fromArray(*((0 until motionBlurIterations).toList().toTypedArray()))
                            .subscribeOn(renderThread)
                            .map { k->
                                Main.arrangementWindow.visualEditor.render(linearInterpolate(frameToTime(frame - 1, bpm.value), frameToTime(frame, bpm.value), (k + 1) / motionBlurIterations.toDouble()), 1).buffer
                            }
                            .map { buffer->
                                Main.opencl.brightPixelsToWhite(buffer, w, h).also { buffer.release() }
                            }
                            .reduce(Main.opencl.createZeroBuffer(w*h*3)) { accum, pixelsBuffer->
                                Main.opencl.pixelSum(accum, pixelsBuffer, w, h, 1.0f/motionBlurIterations)
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
