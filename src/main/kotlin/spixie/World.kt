package spixie

import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.ICodec
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.TreeItem
import javafx.scene.image.ImageView
import spixie.components.ParticleSprayProps
import spixie.components.Root
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class World {
    val root = TreeItem<ComponentsListItem>(Root())
    val frame = Value(0.0, 1.0, "Frame", false)
    val time = Value(0.0, 1.0, "Root Time", false)
    var bpm = Value(140.0, 1.0, "BPM", false)
    @Volatile var allowRender = true
    @Volatile var renderingToFile = false
    @Volatile var imageView:ImageView = ImageView()
    val openCLRenderer:OpenCLRenderer = OpenCLRenderer()
    var currentRenderThread: Thread = Thread(Runnable {
        while (true) {
            try {
                if (allowRender && !renderingToFile) {
                    allowRender = false
                    resizeIfNotCorrect(imageView.fitWidth.toInt(), imageView.fitHeight.toInt())
                    val image = SwingFXUtils.toFXImage(openclRender(), null)
                    Platform.runLater {
                        imageView.image = image
                        //BroadcastRender.renderedImageByteArray = ...
                        allowRender = true
                    }
                }
                Thread.sleep(20)
            } catch (e: ConcurrentModificationException) {
                allowRender = true
                continue
            } catch (e: InterruptedException) {
                return@Runnable
            }
        }
    })

    init {
        val rootTimeChanger = RootTimeChanger(frame, bpm, time)
        frame.item().subscribeChanger(rootTimeChanger)
    }

    fun resizeIfNotCorrect(width: Int, height: Int) {
        if (openCLRenderer.width != width || openCLRenderer.height != height) {
            openCLRenderer.setSize(width,height)
        }
    }

    fun renderStart(imageView: ImageView) {
        allowRender = true
//        BroadcastRender.broadcastRender.start()
        this.imageView = imageView
        currentRenderThread.start()
    }

    private fun openclRender():BufferedImage {
        for (child in Main.world.root.children) {
            val value = child.value
            if(value is ComponentObject){
                if(value.props is ParticleSprayProps){
                    value.props.clearParticles()
                }
            }
        }

        for(particleFrame in 0..Main.world.frame.get().toInt()){
            for (child in Main.world.root.children) {
                val value = child.value
                if(value is ComponentObject){
                    if(value.props is ParticleSprayProps){
                        value.props.stepParticles()
                    }
                }
            }
        }

        val renderBufferBuilder = RenderBufferBuilder()

        for (child in Main.world.root.children) {
            val value = child.value
            if(value is ComponentObject){
                if(value.props is ParticleSprayProps){
                    for (particle in value.props.particles) {
                        renderBufferBuilder.addParticle(particle.x, particle.y, particle.size, particle.red, particle.green, particle.blue, particle.alpha)
                    }
                }
            }
        }

        for (child in Main.world.root.children) {
            val value = child.value
            if(value is ComponentObject){
                value.render(renderBufferBuilder)
            }
        }
        val particlesArray = renderBufferBuilder.toFloatBuffer().array()
        return openCLRenderer.render(particlesArray)
    }

    fun renderToFile(frameRenderedToFileEventHandler: FrameRenderedToFileEvent, renderToFileCompleted: RenderToFileCompleted) {
        renderingToFile = true
        Thread(Runnable {
            openCLRenderer.setSize(1920, 1080)
            val iMediaWriter = ToolFactory.makeWriter("out.mkv")
            iMediaWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_FFVHUFF, 1920, 1080)

            val countFrames = frame.get().toInt() + 1
            for (frame in 0..countFrames - 1) {
                val finalFrame = frame
                val latch = CountDownLatch(1)
                Platform.runLater {
                    this@World.frame.set(finalFrame.toDouble())
                    latch.countDown()
                }
                try {
                    latch.await()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                val bufferedImage = openclRender()
                val bufferedImage1 = BufferedImage(bufferedImage.width, bufferedImage.height, BufferedImage.TYPE_3BYTE_BGR)
                bufferedImage1.graphics.drawImage(bufferedImage, 0, 0, null)
                iMediaWriter.encodeVideo(0, bufferedImage1, Math.round(frame * (1000.0 / 60.0)), TimeUnit.MILLISECONDS)
                Platform.runLater { frameRenderedToFileEventHandler.handle(finalFrame + 1, countFrames) }
            }

            iMediaWriter.close()
            renderingToFile = false
            Platform.runLater { renderToFileCompleted.handle() }
        }).start()
    }

    interface FrameRenderedToFileEvent {
        fun handle(currentFrame: Int, framesCount: Int)
    }

    interface RenderToFileCompleted {
        fun handle()
    }
}
