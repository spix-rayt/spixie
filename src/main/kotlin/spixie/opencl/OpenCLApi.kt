package spixie.opencl

import com.jogamp.opencl.*
import spixie.Main
import spixie.static.roundUp
import java.nio.FloatBuffer
import java.util.concurrent.TimeUnit

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

    fun render(renderBuffer: RenderBuffer, width:Int, height: Int): CLBuffer<FloatBuffer> {
        val tilesCount = ((width+7)/8) * ((height+7)/8)
        val particlesCount = (renderBuffer.particles.capacity()/ RenderBufferBuilder.PARTICLE_FLOAT_SIZE)
        if(particlesCount == 0){
            return createZeroBuffer(width*height*4)
        }

        val outputImage = context.createFloatBuffer(width * height * 4, CLMemory.Mem.READ_WRITE)

        val inputParticles = context.createFloatBuffer(renderBuffer.particles.capacity(), CLMemory.Mem.READ_ONLY)
        inputParticles.buffer.put(renderBuffer.particles)
        inputParticles.buffer.rewind()

        val particleBox = context.createIntBuffer(particlesCount, CLMemory.Mem.READ_WRITE)
        particleBox.buffer.rewind()

        val tiles = context.createIntBuffer(10000000, CLMemory.Mem.READ_WRITE)
        tiles.buffer.rewind()

        val tileStart = context.createIntBuffer(tilesCount, CLMemory.Mem.READ_WRITE)
        tileStart.buffer.rewind()

        val tileSize = context.createIntBuffer(tilesCount, CLMemory.Mem.READ_WRITE)
        tileSize.buffer.rewind()

        val atomicTileIndex = context.createIntBuffer(1, CLMemory.Mem.READ_WRITE)
        atomicTileIndex.buffer.put(0)
        atomicTileIndex.buffer.rewind()

        val atomicParticleIndex = context.createIntBuffer(1, CLMemory.Mem.READ_WRITE)
        atomicParticleIndex.buffer.put(0)
        atomicParticleIndex.buffer.rewind()

        queue.putWriteBuffer(inputParticles, false)
        queue.putWriteBuffer(atomicTileIndex, false)
        queue.putWriteBuffer(atomicParticleIndex, false)

        kotlin.run {
            val kernel = program.createCLKernel("clearTileSize")
            kernel.putArg(tileSize)
            kernel.putArg(width)
            kernel.putArg(height)
            queue.put1DRangeKernel(kernel, 0L, 1L, 1L)
        }

        kotlin.run {
            val kernel = program.createCLKernel("fillTileSize")
            kernel.putArg(inputParticles)
            kernel.putArg(particleBox)
            kernel.putArg(tileSize)
            kernel.putArg(width)
            kernel.putArg(height)
            kernel.putArg(particlesCount)
            kernel.putArg(atomicParticleIndex)
            queue.put1DRangeKernel(kernel, 0L, 51200L, 512L)
        }

        kotlin.run {
            val kernel = program.createCLKernel("fillTileStart")
            kernel.putArg(tileStart)
            kernel.putArg(tileSize)
            kernel.putArg(width)
            kernel.putArg(height)
            queue.put1DRangeKernel(kernel, 0L, 1L, 1L)
        }

        kotlin.run {
            val kernel = program.createCLKernel("fillTiles")
            kernel.putArg(particleBox)
            kernel.putArg(tiles)
            kernel.putArg(tileStart)
            kernel.putArg(width)
            kernel.putArg(height)
            kernel.putArg(particlesCount)
            kernel.putArg(atomicTileIndex)
            queue.put1DRangeKernel(kernel, 0L, 102400L, 1024L)
            queue.putWriteBuffer(atomicTileIndex, false)
        }

        kotlin.run {
            val kernel = program.createCLKernel("renderParticles")
            kernel.putArg(inputParticles)
            kernel.putArg(tiles)
            kernel.putArg(tileStart)
            kernel.putArg(tileSize)
            kernel.putArg(width)
            kernel.putArg(height)
            kernel.putArg(particlesCount)
            kernel.putArg(atomicTileIndex)
            kernel.putArg(outputImage)

            queue.put1DRangeKernel(kernel, 0L, 51200L, 512L)
        }


        inputParticles.release()
        tiles.release()
        tileStart.release()
        tileSize.release()
        atomicTileIndex.release()
        atomicParticleIndex.release()
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