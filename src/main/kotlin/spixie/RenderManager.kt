package spixie

import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
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
    val bpm = ValueControl(160.0, 1.0, "BPM")
    val time = TimeProperty(bpm)
    val offset = ValueControl(0.0, 0.01, "Offset").apply {
        changes.subscribe {
            Main.arrangementWindow.needRedrawWaveform = true
        }
    }
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


    init {
        bpm.apply {
            limitMin(60.0)
            limitMax(999.0)
            changes.subscribe { newBPM->
                Main.arrangementWindow.needRedrawWaveform = true
            }

            Observable.zip(changes, changes.skip(1), BiFunction { previous: Double, current: Double -> previous to current }).subscribe { pair ->
                val t = time.time - offset.value
                offset.value -= t*(pair.second/pair.first)-t
            }
        }
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
                time.time = (frameToTime((seconds*60).roundToInt(), Main.renderManager.bpm.value) + offset.value).coerceAtLeast(0.0)
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

    fun renderToFile(frameRenderedToFileEventHandler: (currentFrame: Int, framesCount: Int) -> Unit, renderToFileCompleted: () -> Unit, motionBlurIterations: Int, startFrame: Int, endFrame: Int, audio: Boolean, offsetAudio: Double) {
        renderingToFile = true
        Thread(Runnable {
            val w = 1920
            val h = 1080
            val fps = 60
            renderer.setSize(w, h)
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
                            "-preset", "slow",
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
                    val resultArray = DoubleArray(w * h * 4)
                    val doubleArray = DoubleArray(w * h * 4)

                    for(k in 0 until motionBlurIterations){
                        val renderedImage = render(Main.arrangementWindow.visualEditor.render(linearInterpolate(frameToTime(frame - 1, bpm.value), frameToTime(frame, bpm.value), (k+1)/motionBlurIterations.toDouble())))
                        renderedImage.raster.getPixels(0,0, renderedImage.width, renderedImage.height, doubleArray)

                        for(x in 0 until w){
                            for (y in 0 until h){
                                val offset = y*w*4+x*4
                                val srcA = doubleArray[offset+3]/motionBlurIterations/255.0
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
