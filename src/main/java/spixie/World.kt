package spixie

import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLDrawableFactory
import com.jogamp.opengl.GLOffscreenAutoDrawable
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.ICodec
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.TreeItem
import javafx.scene.image.ImageView
import spixie.components.Root
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class World {
    val root = TreeItem<ComponentsListItem>(Root())
    val frame = Value(0.0, 1.0, "Frame", false)
    val time = Value(0.0, 1.0, "Root Time", false)
    @Volatile var allowRender = true
    @Volatile var renderingToFile = false
    @Volatile var imageView:ImageView = ImageView()
    var currentRenderThread: Thread = Thread(Runnable {
        val drawable = initGLDrawable()
        while (true) {
            try {
                if (allowRender && !renderingToFile) {
                    allowRender = false
                    resizeIfNotCorrect(drawable, imageView.fitWidth.toInt(), imageView.fitHeight.toInt())
                    val image = SwingFXUtils.toFXImage(openglRender(drawable), null)
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
        val rootTimeChanger = RootTimeChanger(frame, Value(140.0, 1.0, "BPM", false), time)
        frame.item().subscribeChanger(rootTimeChanger)
    }

    fun initGLDrawable(): GLOffscreenAutoDrawable {
        val glProfile = GLProfile.getDefault()
        val glCapabilities = GLCapabilities(glProfile)
        glCapabilities.isOnscreen = false
        glCapabilities.isPBuffer = true
        glCapabilities.sampleBuffers = true
        glCapabilities.numSamples = 1
        val factory = GLDrawableFactory.getFactory(glProfile)
        var drawable = factory.createOffscreenAutoDrawable(null, glCapabilities, null, 1, 1)
        drawable.addGLEventListener(OffscreenGL())
        return drawable
    }

    fun resizeIfNotCorrect(drawable: GLOffscreenAutoDrawable,width: Int, height: Int) {
        if (drawable.surfaceWidth != width || drawable.surfaceHeight != height) {
            drawable.setSurfaceSize(width, height)
        }
    }

    fun renderStart(imageView: ImageView) {
        allowRender = true
//        BroadcastRender.broadcastRender.start()
        this.imageView = imageView
        currentRenderThread.start()
    }

    private fun openglRender(drawable: GLOffscreenAutoDrawable): BufferedImage {
        drawable.display()
        return AWTGLReadBufferUtil(drawable.glProfile, true).readPixelsToBufferedImage(drawable.gl, true)
    }

    fun renderToFile(frameRenderedToFileEventHandler: FrameRenderedToFileEvent, renderToFileCompleted: RenderToFileCompleted) {
        renderingToFile = true
        Thread(Runnable {
            val glp = GLProfile.getDefault()
            val glc = GLCapabilities(glp)
            glc.isOnscreen = false
            glc.isPBuffer = true
            glc.sampleBuffers = true
            glc.numSamples = 16
            val fc = GLDrawableFactory.getFactory(glp)
            val offscreenAutoDrawable = fc.createOffscreenAutoDrawable(null, glc, null, 1920, 1024)
            offscreenAutoDrawable.addGLEventListener(OffscreenGL())

            val iMediaWriter = ToolFactory.makeWriter("out.mp4")
            iMediaWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, 1920, 1024)

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

                val bufferedImage = openglRender(offscreenAutoDrawable)
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
