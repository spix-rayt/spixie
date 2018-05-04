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
import spixie.static.Settings
import spixie.static.printAvailable
import spixie.static.runInUIAndWait
import spixie.visual_editor.ParticleArray
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO

class RenderManager {
    val time = TimeProperty(160.0)
    var bpm = ValueControl(160.0, 1.0, "BPM")
    @Volatile var renderingToFile = false
    private val openCLRenderer:OpenCLRenderer = OpenCLRenderer()
    private val cache = ReferenceMap<Long, AsyncPngConvert>()
    private val frameCache = ReferenceMap<Double, AsyncPngConvert>()
    val forceRender = BehaviorSubject.createDefault(Unit)
    var autoRenderNextFrame = false

    var scaleDown:Int = 2
        set(value) {
            field = value
            clearCache()
        }

    private fun resizeIfNotCorrect(width: Int, height: Int) {
        if (openCLRenderer.width != width || openCLRenderer.height != height) {
            openCLRenderer.setSize(width,height)
        }
    }

    val renderThread = Schedulers.newThread()

    var perFrame = {  }

    fun renderStart(imageView: ImageView) {
        perFrame = {
            if(Main.audio.isPlaying()){
                val seconds = Main.audio.getTime()
                time.time = Main.renderManager.bpm.value.value/3600*seconds*60
                frameCache[Main.renderManager.bpm.value.value/3600*(seconds*60).toInt()]?.get()?.let {
                    imageView.image = Image(ByteArrayInputStream(it))
                }
            }
        }

        Flowable.combineLatest(time.timeChanges.toFlowable(BackpressureStrategy.LATEST), forceRender.toFlowable(BackpressureStrategy.LATEST), BiFunction { t:Double, _:Unit -> t })
                .observeOn(renderThread)
                .subscribe { t ->
                    try {
                        if (!renderingToFile && !Main.audio.isPlaying()) {
                            val particles = Main.workingWindow.arrangementWindow.visualEditor.resultComponent.getParticles()
                            val cachedImageAsync = cache[particles.hash]
                            val imageCached = cachedImageAsync?.get()
                            if(imageCached == null){
                                resizeIfNotCorrect(imageView.fitWidth.toInt()/scaleDown, imageView.fitHeight.toInt()/scaleDown)
                                val bufferedImage = openclRender(particles)
                                val image = SwingFXUtils.toFXImage(bufferedImage, null)
                                val asyncPng = AsyncPngConvert(bufferedImage)
                                cache.put(particles.hash, asyncPng)
                                frameCache.put(t, asyncPng)
                                runInUIAndWait {
                                    imageView.image = image
                                }
                            }else{
                                val toFXImage = Image(ByteArrayInputStream(imageCached))
                                frameCache.put(t, cachedImageAsync)
                                runInUIAndWait {
                                    imageView.image = toFXImage
                                }
                            }
                            if(autoRenderNextFrame){
                                runInUIAndWait {
                                    time.frame += 1
                                }
                            }
                        }
                    } catch (e: ConcurrentModificationException) {

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
                            "-c:v", "libx264",
                            "-preset", "slow",
                            "-crf", "17",
                            "-pix_fmt", "yuv420p",
                            "out.mp4"
                    )
            )
            val process = processBuilder.start()
            val outputStream = process.outputStream
            val inputStream = process.inputStream
            val errorStream = process.errorStream

            for (frame in 0 until countFrames) {
                runInUIAndWait {
                    this@RenderManager.time.frame = frame
                }
                if(!process.isAlive){
                    break
                }

                val bufferedImage = openclRender(Main.workingWindow.arrangementWindow.visualEditor.resultComponent.getParticles())
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
        }.subscribeOn(renderThread).subscribe()
    }

    interface FrameRenderedToFileEvent {
        fun handle(currentFrame: Int, framesCount: Int)
    }

    interface RenderToFileCompleted {
        fun handle()
    }
}
