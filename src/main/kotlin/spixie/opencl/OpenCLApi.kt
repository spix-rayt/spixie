package spixie.opencl

import com.jogamp.opencl.*
import spixie.static.roundUp
import java.nio.FloatBuffer
import java.util.*

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

    fun brightPixelsToWhite(inputImage: CLBuffer<FloatBuffer>, width:Int, height: Int): CLBuffer<FloatBuffer> {
        val outputImage = context.createFloatBuffer(width * height * 3, CLMemory.Mem.READ_WRITE)

        val kernel = program.createCLKernel("brightPixelsToWhite")
        kernel.putArg(inputImage)
        kernel.putArg(width*height)
        kernel.putArg(outputImage)

        queue.put1DRangeKernel(kernel, 0L, (width*height).roundUp(64).toLong(), 64L)

        return outputImage
    }

    fun forSave(inputImage: CLBuffer<FloatBuffer>, width:Int, height: Int): CLBuffer<FloatBuffer> {
        val outputImage = context.createFloatBuffer(width * height * 3, CLMemory.Mem.READ_WRITE)

        val kernel = program.createCLKernel("forSave")
        kernel.putArg(inputImage)
        kernel.putArg(width*height*3)
        kernel.putArg(outputImage)

        queue.put1DRangeKernel(kernel, 0L, (width*height*3).roundUp(64).toLong(), 64L)

        return outputImage
    }

    fun createZeroBuffer(size:Int): CLBuffer<FloatBuffer> {
        val buffer = context.createFloatBuffer(size, CLMemory.Mem.READ_WRITE)
        val kernel = program.createCLKernel("zeroBuffer")
        kernel.putArg(buffer)
        kernel.putArg(size)
        queue.put1DRangeKernel(kernel, 0L, size.roundUp(64).toLong(), 64L)
        return buffer
    }

    fun pixelSum(accumImage: CLBuffer<FloatBuffer>, addImage: CLBuffer<FloatBuffer>, width:Int, height: Int, k: Float) {
        val kernel = program.createCLKernel("pixelSum")
        kernel.putArg(accumImage)
        kernel.putArg(addImage)
        kernel.putArg(k)
        kernel.putArg(width*height*3)

        queue.put1DRangeKernel(kernel, 0L, (width*height*3).roundUp(64).toLong(), 64L)
    }

    fun readAndRelease(buffer: CLBuffer<FloatBuffer>): FloatArray {
        queue.putReadBuffer(buffer, true)
        val result = FloatArray(buffer.buffer.capacity())
        buffer.buffer.get(result)
        buffer.release()
        return result
    }

    fun renderSplats(floatArray: FloatArray, objectsCount: Int, width: Int, height: Int, equirectangular: Int, vr: Int, screenWidth: Float, screenHeight: Float, screenDistance: Float): CLBuffer<FloatBuffer> {
        val outputImage = context.createFloatBuffer(width * height * 4, CLMemory.Mem.READ_WRITE)

        val inputFloatArray = if(objectsCount == 0) {
            context.createFloatBuffer(1, CLMemory.Mem.READ_ONLY)
        } else {
            context.createFloatBuffer(floatArray.size, CLMemory.Mem.READ_ONLY)
        }
        inputFloatArray.buffer.put(floatArray)
        inputFloatArray.buffer.rewind()

        run {
            val kernel = program.createCLKernel("render")
            kernel.putArg(inputFloatArray)
            kernel.putArg(objectsCount)
            kernel.putArg(width)
            kernel.putArg(height)
            kernel.putArg(equirectangular)
            kernel.putArg(vr)
            kernel.putArg(screenWidth)
            kernel.putArg(screenHeight)
            kernel.putArg(screenDistance)
            kernel.putArg(outputImage)

            queue.putWriteBuffer(inputFloatArray, false)
            queue.put1DRangeKernel(kernel, 0L, (width * height).roundUp(256).toLong(), 256L)
        }
        return outputImage
    }
}