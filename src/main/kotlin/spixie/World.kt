package spixie

import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.apache.commons.collections4.map.ReferenceMap
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO

class World {
    val time = TimeProperty(160.0)
    var bpm = ValueControl(160.0, 1.0, "BPM")
    @Volatile var renderingToFile = false
    var imageView:ImageView = ImageView()
    val openCLRenderer:OpenCLRenderer = OpenCLRenderer()
    private val cache = ReferenceMap<Long, DeferredPng>()
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
                        Main.workingWindow.resetCurrentFrameCache()
                        frameHashShown=0L
                        needClearCache=false
                    }
                    if(Main.audio.isPlayed()){
                        runInUIAndWait {
                            val newTime = Main.audio.getTime()
                            if(newTime!= time.time){
                                time.time=newTime
                            }
                        }
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
                        val imageCached = cache.get(spixieHash)?.get()
                        if(imageCached == null){
                            resizeIfNotCorrect(imageView.fitWidth.toInt()/scaleDown, imageView.fitHeight.toInt()/scaleDown)
                            val frameBeforeRender = time.frame
                            val bufferedImage = openclRender()
                            val image = SwingFXUtils.toFXImage(bufferedImage, null)
                            if(frameBeforeRender == time.frame){
                                cache.put(spixieHash, DeferredPng(bufferedImage))
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
        val renderBufferBuilder = RenderBufferBuilder(openCLRenderer.width, openCLRenderer.height, openCLRenderer.BLOCKSIZE)

        for (block in Main.workingWindow.arrangementWindow.blocks.children) {
            if(block is ArrangementBlock){
                block.render(renderBufferBuilder)
            }
        }

        return openCLRenderer.render(renderBufferBuilder)
    }

    fun renderToFile(frameRenderedToFileEventHandler: FrameRenderedToFileEvent, renderToFileCompleted: RenderToFileCompleted) {
        renderingToFile = true
        Thread(Runnable {
            openCLRenderer.setSize(1920, 1080)
            val countFrames = time.frame + 1
            val processBuilder = ProcessBuilder(
                    listOf(
                            Main.settings.ffmpeg,
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
                    this@World.time.frame = frame
                }
                if(!process.isAlive){
                    break
                }

                val bufferedImage = openclRender()
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

    private var needClearCache = false
    fun clearCache(){
        needClearCache = true
    }

    fun calcSpixieHash():Long {
        return Main.workingWindow.arrangementWindow.spixieHash() mix time.frame.toLong()
    }

    interface FrameRenderedToFileEvent {
        fun handle(currentFrame: Int, framesCount: Int)
    }

    interface RenderToFileCompleted {
        fun handle()
    }
}
