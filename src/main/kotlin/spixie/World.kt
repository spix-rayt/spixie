package spixie

import io.humble.video.*
import io.humble.video.awt.MediaPictureConverter
import io.humble.video.awt.MediaPictureConverterFactory
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.apache.commons.collections4.map.ReferenceMap
import spixie.components.Circle
import spixie.components.ParticleSpray
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO

class World {
    val time = TimeProperty(140.0)
    var bpm = Value(140.0, 1.0, "BPM", false)
    @Volatile var renderingToFile = false
    var imageView:ImageView = ImageView()
    val openCLRenderer:OpenCLRenderer = OpenCLRenderer()
    private val cache = ReferenceMap<Long, ByteArray>()
    private var frameHashShown = 0L
    var autoRenderNextFrame = false

    var scaleDown:Int = 2
        set(value) {
            field = value
            clearCache()
        }

    var currentRenderThread: Thread = Thread(Runnable {
        while (true) {
            try {
                if (!renderingToFile) {
                    if(needClearCache){
                        cache.clear()
                        frameHashShown=0
                        needClearCache=false
                    }
                    val spixieHash = calcSpixieHash()
                    if(frameHashShown == spixieHash){
                        if(autoRenderNextFrame){
                            runInUIAndWait {
                                time.frame += 1
                            }
                        }else{
                            Thread.sleep(1)
                        }
                    }else{
                        val imageCached = cache.get(spixieHash)
                        if(imageCached == null){
                            resizeIfNotCorrect(imageView.fitWidth.toInt()/scaleDown, imageView.fitHeight.toInt()/scaleDown)
                            val frameBeforeRender = time.frame
                            val bufferedImage = openclRender()
                            val image = SwingFXUtils.toFXImage(bufferedImage, null)
                            if(frameBeforeRender == time.frame){
                                cache.put(spixieHash, imageToPngByteArray(bufferedImage))
                                frameHashShown = spixieHash
                            }
                            runInUIAndWait {
                                imageView.image = image
                            }
                        }else{
                            frameHashShown = spixieHash
                            val toFXImage = Image(ByteArrayInputStream(imageCached))
                            runInUIAndWait {
                                imageView.image = toFXImage
                            }
                        }
                    }
                }
            } catch (e: ConcurrentModificationException) {
                continue
            } catch (e: InterruptedException) {
                return@Runnable
            }
        }
    })

    fun imageToPngByteArray(bufferedImage: BufferedImage):ByteArray{
        val byteArrayOutputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun resizeIfNotCorrect(width: Int, height: Int) {
        if (openCLRenderer.width != width || openCLRenderer.height != height) {
            openCLRenderer.setSize(width,height)
        }
    }

    fun renderStart(imageView: ImageView) {
        this.imageView = imageView
        currentRenderThread.start()
    }

    private fun openclRender():BufferedImage {
        val renderBufferBuilder = RenderBufferBuilder()
        for (block in Main.workingWindow.arrangementWindow.blocks.children) {
            if(block is ArrangementBlock){
                for (component in block.visualEditor.components.children) {
                    if(component is ParticleSpray){
                        component.clearParticles()
                    }
                }
            }
        }
        for(particleFrame in 0..Main.world.time.frame){
            for (block in Main.workingWindow.arrangementWindow.blocks.children) {
                if(block is ArrangementBlock){
                    for (component in block.visualEditor.components.children) {
                        if(component is ParticleSpray){
                            component.stepParticles()
                        }
                    }
                }
            }
        }

        for (block in Main.workingWindow.arrangementWindow.blocks.children) {
            if(block is ArrangementBlock){
                for (component in block.visualEditor.components.children) {
                    if(component is ParticleSpray){
                        for (particle in component.particles) {
                            renderBufferBuilder.addParticle(particle.x, particle.y, particle.size, particle.red, particle.green, particle.blue, particle.alpha)
                        }
                    }
                    if(component is Circle){
                        component.render(renderBufferBuilder)
                    }
                }
            }
        }

        return openCLRenderer.render(renderBufferBuilder)
    }

    fun renderToFile(frameRenderedToFileEventHandler: FrameRenderedToFileEvent, renderToFileCompleted: RenderToFileCompleted) {
        renderingToFile = true
        Thread(Runnable {
            openCLRenderer.setSize(1920, 1080)
            val frameRate = Rational.make(1, 60)
            val muxer = Muxer.make("out.mkv", null, "matroska")
            val codec = Codec.findEncodingCodecByName("ffv1")
            val encoder = Encoder.make(codec)
            encoder.width = 1920
            encoder.height = 1080
            encoder.pixelFormat = PixelFormat.Type.PIX_FMT_YUV420P
            encoder.timeBase = frameRate
            encoder.setFlag(Coder.Flag.FLAG_GLOBAL_HEADER, true)

            encoder.open(null, null)
            muxer.addNewStream(encoder)
            muxer.open(null, null)

            var converter: MediaPictureConverter? = null
            val picture = MediaPicture.make(encoder.width, encoder.height, encoder.pixelFormat)
            picture.timeBase = frameRate

            val packet = MediaPacket.make()

            val countFrames = time.frame + 1
            for (frame in 0 until countFrames) {
                runInUIAndWait {
                    this@World.time.frame = frame
                }

                val bufferedImage = openclRender()
                val bufferedImage1 = BufferedImage(bufferedImage.width, bufferedImage.height, BufferedImage.TYPE_3BYTE_BGR)
                bufferedImage1.graphics.drawImage(bufferedImage, 0, 0, null)

                if(converter == null){
                    converter = MediaPictureConverterFactory.createConverter(bufferedImage1, picture)
                }
                converter!!.toPicture(picture, bufferedImage1, frame.toLong())

                do{
                    encoder.encode(packet, picture)
                    if(packet.isComplete){
                        muxer.write(packet, false)
                    }
                }while (packet.isComplete)


                Platform.runLater { frameRenderedToFileEventHandler.handle(frame + 1, countFrames) }
            }
            do{
                encoder.encode(packet, picture)
                if(packet.isComplete){
                    muxer.write(packet, false)
                }
            }while (packet.isComplete)
            muxer.close()
            renderingToFile = false
            Platform.runLater { renderToFileCompleted.handle() }
        }).start()
    }

    fun runInUIAndWait(work: () -> Unit){
        val latch = CountDownLatch(1)
        Platform.runLater {
            work()
            latch.countDown()
        }
        latch.await()
    }

    private var needClearCache = false
    fun clearCache(){
        needClearCache = true
    }

    fun calcSpixieHash():Long {
        return Main.workingWindow.arrangementWindow.spixieHash() mix time.frame.toDouble().raw()
    }

    interface FrameRenderedToFileEvent {
        fun handle(currentFrame: Int, framesCount: Int)
    }

    interface RenderToFileCompleted {
        fun handle()
    }
}
