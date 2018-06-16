package spixie.opencl

import com.jogamp.opencl.*
import spixie.Main
import spixie.static.roundUp
import java.nio.FloatBuffer

class OpenCLApi {
    private val context:CLContext = CLContext.create(
            CLPlatform.listCLPlatforms().maxBy {
                when{
                    it.name.toLowerCase().contains("intel") -> 0
                    else -> 42 // Prefer not intel GPU
                }
            }
    )
    private val device:CLDevice
    private val program:CLProgram
    private val queue: CLCommandQueue

    init {
        device = context.maxFlopsDevice
        queue = device.createCommandQueue()
        println(device)
        program = context.createProgram(javaClass.getResourceAsStream("/kernel.cl")).build()
    }

    fun render(particlesArray:FloatBuffer, realWidth:Int, realHeight: Int): CLBuffer<FloatBuffer> {
        val width=realWidth.roundUp(64)
        val height=realHeight

        val outputImage = context.createFloatBuffer(realWidth * realHeight * 4, CLMemory.Mem.READ_WRITE)

        val particlesCount = (particlesArray.capacity()/ RenderBufferBuilder.PARTICLE_FLOAT_SIZE).roundUp(64)
        if(particlesCount == 0){
            return createZeroBuffer(realWidth*realHeight*4)
        }

        val inputParticles = context.createFloatBuffer(particlesCount * RenderBufferBuilder.PARTICLE_FLOAT_SIZE, CLMemory.Mem.READ_ONLY)


        inputParticles.buffer.put(particlesArray)
        inputParticles.buffer.rewind()

        val kernel = program.createCLKernel("renderParticles")
        kernel.putArgs(inputParticles)
        kernel.putArg(width)
        kernel.putArg(height)
        kernel.putArg(realWidth)
        kernel.putArg(particlesCount)
        kernel.putArgs(outputImage)

        queue.putWriteBuffer(inputParticles, false)
                .put1DRangeKernel(kernel, 0L, (width * height).toLong(), 64L)
        inputParticles.release()
        return outputImage
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
        Main.opencl.queue.putReadBuffer(buffer, true)
        val result = FloatArray(buffer.buffer.capacity())
        buffer.buffer.get(result)
        buffer.release()
        return result
    }
}