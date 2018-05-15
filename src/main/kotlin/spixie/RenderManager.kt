package spixie

import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.rxjavafx.observables.JavaFxObservable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform
import javafx.geometry.Bounds
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.apache.commons.collections4.map.ReferenceMap
import spixie.renderer.AparapiRenderer
import spixie.renderer.RenderBufferBuilder
import spixie.renderer.Renderer
import spixie.static.*
import spixie.visualEditor.ParticleArray
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class RenderManager {
    val time = TimeProperty(160.0)
    val bpm = ValueControl(160.0, 1.0, "BPM")
    @Volatile private var renderingToFile = false
    private val renderer: Renderer = AparapiRenderer()
    private val cache = ReferenceMap<Long, ByteArray>()
    private val frameCache = ReferenceMap<Double, ByteArray>()
    private val forceRender = BehaviorSubject.createDefault(Unit).toSerialized()
    @Volatile var autoRenderNextFrame = false

    var scaleDown:Int = 2
        set(value) {
            field = value
            requestRender()
        }

    private fun resizeIfNotCorrect(width: Int, height: Int) {
        val (currentWidth, currentHeight) = renderer.getSize()
        if (currentWidth != width || currentHeight != height) {
            renderer.setSize(width,height)
            cache.clear()
            frameCache.clear()
        }
    }

    private val renderThread = Schedulers.newThread()

    var perFrame = {  }

    fun renderStart(imageView: ImageView) {
        perFrame = {
            if(Main.audio.isPlaying()){
                val seconds = Main.audio.getTime()
                time.time = frameToTime((seconds*60).roundToInt(), Main.renderManager.bpm.value)
                frameCache[time.time]?.let {
                    val byteArrayInputStream = ByteArrayInputStream(it)
                    imageView.image = Image(byteArrayInputStream)
                }
            }
        }

        Observable.combineLatest(time.timeChanges.distinctUntilChanged(), forceRender, JavaFxObservable.valuesOf(imageView.layoutBoundsProperty()), Function3 { t:Double, _:Unit, bounds:Bounds -> t to bounds })
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(renderThread, false, 1)
                .subscribe { (t, bounds) ->
                    try {
                        if (!renderingToFile && !Main.audio.isPlaying()) {
                            resizeIfNotCorrect(bounds.width.toInt()/scaleDown, bounds.height.toInt()/scaleDown)
                            val particles = Main.arrangementWindow.visualEditor.render(t)
                            val imageCached = cache[particles.hash]
                            if(imageCached == null){
                                val image = render(particles).toPNGByteArray()
                                cache[particles.hash] = image
                                frameCache[t] = image
                            }else{
                                frameCache[t] = imageCached
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
                        requestRender()
                    } catch (e: StackOverflowError) {
                        println("Stack overflow")
                    }
                }
    }

    fun requestRender(){
        forceRender.onNext(Unit)
    }

    private fun render(particleArray: ParticleArray):BufferedImage {
        val renderBufferBuilder = RenderBufferBuilder(particleArray.array.size)
        particleArray.array.forEach { particle ->
            if(particle.matrix.m32()>=-960){
                renderBufferBuilder.addParticle(particle.matrix.m30()/((particle.matrix.m32()+1000)/1000), particle.matrix.m31()/((particle.matrix.m32()+1000)/1000), particle.size/((particle.matrix.m32()+1000)/1000), particle.red, particle.green, particle.blue, particle.alpha)
            }
        }
        return renderer.render(renderBufferBuilder.complete())
    }

    fun renderToFile(frameRenderedToFileEventHandler: (currentFrame: Int, framesCount: Int) -> Unit, renderToFileCompleted: () -> Unit) {
        renderingToFile = true
        Thread(Runnable {
            renderer.setSize(1920, 1080)
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
                val (w, h) = renderer.getSize()
                val resultArray = DoubleArray(w * h * 4)
                val doubleArray = DoubleArray(w * h * 4)
                val iterations = 3

                for(k in 0 until iterations){
                    val renderedImage = render(Main.arrangementWindow.visualEditor.render(linearInterpolate(frameToTime(frame - 1, bpm.value), frameToTime(frame, bpm.value), (k+1)/iterations.toDouble())))
                    renderedImage.raster.getPixels(0,0, renderedImage.width, renderedImage.height, doubleArray)

                    for(x in 0 until w){
                        for (y in 0 until h){
                            val offset = y*w*4+x*4
                            val srcA = doubleArray[offset+3]/iterations/255.0
                            resultArray[offset] = srcA*doubleArray[offset] + resultArray[offset]
                            resultArray[offset+1] = srcA*doubleArray[offset+1] + resultArray[offset+1]
                            resultArray[offset+2] = srcA*doubleArray[offset+2] + resultArray[offset+2]
                            resultArray[offset+3] = 255.0
                        }
                    }
                }
                val bufferedImage = BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR)
                bufferedImage.raster.setPixels(0, 0, w, h, resultArray)
                val bufferedImage1 = BufferedImage(bufferedImage.width, bufferedImage.height, BufferedImage.TYPE_3BYTE_BGR)
                bufferedImage1.graphics.drawImage(bufferedImage, 0, 0, null)

                ImageIO.write(bufferedImage1, "png", outputStream)

                inputStream.printAvailable()
                errorStream.printAvailable()

                Platform.runLater { frameRenderedToFileEventHandler(frame + 1, countFrames) }
            }
            outputStream.close()
            process.waitFor()
            inputStream.printAvailable()
            errorStream.printAvailable()
            println("Process finished with exit code ${process.exitValue()}")
            renderingToFile = false
            Platform.runLater { renderToFileCompleted() }
        }).start()
    }

    fun clearCache(){
        Completable.fromAction {
            cache.clear()
            frameCache.clear()
            requestRender()
        }.subscribeOn(renderThread).subscribe()
    }
}
