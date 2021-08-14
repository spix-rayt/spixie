package spixie.render

import com.jogamp.opencl.*
import org.joml.Vector2f
import org.joml.Vector3f
import spixie.static.map
import spixie.static.roundUp
import java.awt.image.BufferedImage
import java.nio.FloatBuffer
import java.util.*
import kotlin.math.max

class OpenCLApi {
    private val context:CLContext = CLContext.create(
            CLPlatform.listCLPlatforms().maxByOrNull {
                when{
                    it.name.lowercase(Locale.getDefault()).contains("intel") -> 0
                    else -> 42 // Prefer not intel GPU
                }
            }
    )

    private val device: CLDevice

    private val program: CLProgram

    private val queue: CLCommandQueue

    init {
        device = context.maxFlopsDevice
        queue = device.createCommandQueue()
        println(device)
        val resourceAsStream = javaClass.getResourceAsStream("/kernel.cl")
        program = context.createProgram(resourceAsStream).build()
    }

    val splatsFloatArray by lazy { context.createFloatBuffer(700000, CLMemory.Mem.READ_ONLY) }

    val kernelBrightPixelsToWhite by lazy { program.createCLKernel("brightPixelsToWhite") }
    val kernelPrepareForSave by lazy { program.createCLKernel("prepareForSave") }
    val kernelZeroBuffer by lazy { program.createCLKernel("zeroBuffer") }
    val kernelRender by lazy { program.createCLKernel("render") }

    fun createImageCLBuffer(width: Int, height: Int): ImageCLBuffer {
        return ImageCLBuffer(createZeroBuffer(width * height * 3), width, height)
    }

    fun brightPixelsToWhite(inputImage: CLBuffer<FloatBuffer>, width:Int, height: Int, k: Float = 1.0f): CLBuffer<FloatBuffer> {
        val outputImage = context.createFloatBuffer(width * height * 3, CLMemory.Mem.READ_WRITE)

        kernelBrightPixelsToWhite.rewind()
        kernelBrightPixelsToWhite.putArg(inputImage)
        kernelBrightPixelsToWhite.putArg(width*height)
        kernelBrightPixelsToWhite.putArg(k)
        kernelBrightPixelsToWhite.putArg(outputImage)

        queue.put1DRangeKernel(kernelBrightPixelsToWhite, 0L, (width*height).roundUp(64).toLong(), 64L)

        return outputImage
    }

    fun prepareForSave(inputImage: CLBuffer<FloatBuffer>, width:Int, height: Int): CLBuffer<FloatBuffer> {
        val outputImage = context.createFloatBuffer(width * height * 3, CLMemory.Mem.READ_WRITE)

        kernelPrepareForSave.rewind()
        kernelPrepareForSave.putArg(inputImage)
        kernelPrepareForSave.putArg(width * height * 3)
        kernelPrepareForSave.putArg(outputImage)

        queue.put1DRangeKernel(kernelPrepareForSave, 0L, (width * height * 3).roundUp(64).toLong(), 64L)

        return outputImage
    }

    fun createZeroBuffer(size:Int): CLBuffer<FloatBuffer> {
        val buffer = context.createFloatBuffer(size, CLMemory.Mem.READ_WRITE)
        kernelZeroBuffer.rewind()
        kernelZeroBuffer.putArg(buffer)
        kernelZeroBuffer.putArg(size)
        queue.put1DRangeKernel(kernelZeroBuffer, 0L, size.roundUp(64).toLong(), 64L)
        return buffer
    }

    fun readAndRelease(buffer: CLBuffer<FloatBuffer>): FloatArray {
        queue.putReadBuffer(buffer, true)
        val result = FloatArray(buffer.buffer.capacity())
        buffer.buffer.get(result)
        buffer.release()
        return result
    }

    private val CHUNK_SIZE = 64

    fun renderSplats(imageCLBuffer: ImageCLBuffer, splats: List<Splat>, renderParameters: RenderParameters) {
        if(splats.isEmpty()) {
            return
        }
        val sortedSplats = splats.sortedBy { it.x * it.x + it.y * it.y + it.z * it.z }
        for(chunkY in 0..imageCLBuffer.height step CHUNK_SIZE) {
            for(chunkX in 0..imageCLBuffer.width step CHUNK_SIZE) {
                val filteredSplats = filterSplatsForChunk(imageCLBuffer, sortedSplats, renderParameters, chunkX, chunkY)

                var splatsCount = 0
                splatsFloatArray.buffer.rewind()
                filteredSplats.forEach { splat ->
                    splatsCount++
                    splatsFloatArray.buffer.put(splat.x)
                    splatsFloatArray.buffer.put(splat.y)
                    splatsFloatArray.buffer.put(splat.z)
                    splatsFloatArray.buffer.put(splat.size)
                    splatsFloatArray.buffer.put(splat.r)
                    splatsFloatArray.buffer.put(splat.g)
                    splatsFloatArray.buffer.put(splat.b)
                }
                splatsFloatArray.buffer.rewind()
                chunkRenderSplats(imageCLBuffer, splatsCount, renderParameters, chunkX, chunkY)
            }
        }
    }

    private fun filterSplatsForChunk(imageCLBuffer: ImageCLBuffer, sortedSplats: List<Splat>, renderParameters: RenderParameters, chunkX: Int, chunkY: Int): Sequence<Splat> {
        val coordX = chunkX + CHUNK_SIZE.toFloat() / 2.0f - 0.5f
        val coordY = chunkY + CHUNK_SIZE.toFloat() / 2.0f - 0.5f
        val uv: Vector2f
        if(renderParameters.vr == 1) {
            uv = Vector2f((coordX % (imageCLBuffer.width / 2)) / (imageCLBuffer.width / 2).toFloat(), coordY / imageCLBuffer.height)
        } else {
            uv = Vector2f(coordX / imageCLBuffer.width, coordY / imageCLBuffer.height)
        }
        val rayOrigin = Vector3f(0.0f, 0.0f, 0.0f)
        val rayDirection: Vector3f

        val screenWidth = imageCLBuffer.width.toFloat() / imageCLBuffer.height.toFloat() * renderParameters.screenSize
        val screenHeight = renderParameters.screenSize

        rayDirection = Vector3f(
            map(-screenWidth, screenWidth, uv.x),
            map(-screenHeight, screenHeight, uv.y),
            renderParameters.screenDistance
        ).normalize()

        return sortedSplats.asSequence().filter { splat ->
            val pos = Vector3f(splat.x, splat.y, splat.z)
            val dt = max(rayDirection.dot(pos), 0.0f)
            val projection = Vector3f(rayOrigin).add(Vector3f(rayDirection).mul(dt))
            val dist = projection.sub(pos).length()
            val resolutionFactor = max(1920.0 / imageCLBuffer.width, 1080.0 / imageCLBuffer.height)
            dist < splat.size + 0.0065f * dt * 4.5f * resolutionFactor //FIXME: improve calculations
        }.take(100000)
    }

    private fun chunkRenderSplats(imageCLBuffer: ImageCLBuffer, splatsCount: Int, renderParameters: RenderParameters, chunkX: Int, chunkY: Int) {
        kernelRender.rewind()
        kernelRender.putArg(splatsFloatArray)
        kernelRender.putArg(splatsCount)
        kernelRender.putArg(imageCLBuffer.width)
        kernelRender.putArg(imageCLBuffer.height)
        kernelRender.putArg(renderParameters.equirectangular)
        kernelRender.putArg(renderParameters.vr)
        kernelRender.putArg(renderParameters.screenSize)
        kernelRender.putArg(renderParameters.screenDistance)
        kernelRender.putArg(renderParameters.samplesPerPixel)
        kernelRender.putArg(chunkX)
        kernelRender.putArg(chunkY)
        kernelRender.putArg(imageCLBuffer.buffer)

        queue.putWriteBuffer(splatsFloatArray, false)
        queue.put1DRangeKernel(kernelRender, 0L, (CHUNK_SIZE * CHUNK_SIZE).toLong(), 256L)
    }


    fun clBufferToBufferedImage(buffer: CLBuffer<FloatBuffer>, width: Int, height: Int): BufferedImage {
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
        val bufferPreparedForSave = prepareForSave(buffer, width, height)
        buffer.release()
        val floatArray = readAndRelease(bufferPreparedForSave)
        bufferedImage.raster.setPixels(0, 0, width, height, floatArray)
        return bufferedImage
    }

    fun imageCLBufferToBufferedImage(imageCLBuffer: ImageCLBuffer): BufferedImage {
        val newBuffer = brightPixelsToWhite(imageCLBuffer.buffer, imageCLBuffer.width, imageCLBuffer.height)
        imageCLBuffer.buffer.release()
        return clBufferToBufferedImage(newBuffer, imageCLBuffer.width, imageCLBuffer.height)
    }
}