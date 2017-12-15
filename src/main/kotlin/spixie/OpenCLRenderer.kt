package spixie

import com.jogamp.opencl.*
import java.awt.image.BufferedImage
import java.nio.FloatBuffer

class OpenCLRenderer {
    val context:CLContext
    val device:CLDevice
    val program:CLProgram
    val clImageOut:CLBuffer<FloatBuffer>
    var width = 1
    var height = 1

    val BLOCKSIZE = 200

    init {
        context = CLContext.create()
        device = context.maxFlopsDevice
        program = context.createProgram(javaClass.getResourceAsStream("/kernel.cl")).build()
        clImageOut = context.createFloatBuffer(BLOCKSIZE*BLOCKSIZE*4, CLMemory.Mem.WRITE_ONLY)
    }

    fun render(renderBufferBuilder:RenderBufferBuilder): BufferedImage {
        val queue = device.createCommandQueue()
        val localWorkSize = minOf(device.maxWorkGroupSize, 256)
        val bufferedImage = BufferedImage(width + (BLOCKSIZE - width%BLOCKSIZE), height + (BLOCKSIZE - height%BLOCKSIZE), BufferedImage.TYPE_4BYTE_ABGR)
        val raster = bufferedImage.raster

        for (x in 0..width/BLOCKSIZE){
            for (y in 0..height/BLOCKSIZE){
                val blockX = x * BLOCKSIZE
                val blockY = y * BLOCKSIZE

                val particlesArray = renderBufferBuilder.toFloatBuffer(blockX, blockY, BLOCKSIZE, width, height).array()
                val particlesCount = particlesArray.size/RenderBufferBuilder.PARTICLE_FLOAT_SIZE
                if(particlesCount==0){
                    continue
                }

                val clParticles = context.createFloatBuffer(particlesCount*RenderBufferBuilder.PARTICLE_FLOAT_SIZE, CLMemory.Mem.READ_ONLY)


                clParticles.buffer.put(particlesArray)
                clParticles.buffer.rewind()
                clImageOut.buffer.rewind()

                val kernel = program.createCLKernel("RenderParticles")
                kernel.putArgs(clParticles)
                kernel.putArg(width)
                kernel.putArg(height)
                kernel.putArg(BLOCKSIZE)
                kernel.putArg(blockX)
                kernel.putArg(blockY)
                kernel.putArg(particlesCount)
                kernel.putArgs(clImageOut)

                queue.putWriteBuffer(clParticles, false)
                        .put1DRangeKernel(kernel, 0L, roundUp(localWorkSize, BLOCKSIZE * BLOCKSIZE).toLong(), localWorkSize.toLong())
                        .putReadBuffer(clImageOut, true)



                val intArray = IntArray(BLOCKSIZE*BLOCKSIZE*4)
                for(i in 0 until BLOCKSIZE*BLOCKSIZE*4){
                    val round = Math.round(clImageOut.buffer.get() * 255)
                    intArray[i] = Math.max(0, Math.min(round, 255))
                }

                raster.setPixels(blockX, blockY, BLOCKSIZE, BLOCKSIZE, intArray)
                bufferedImage.data = raster
                clParticles.release()
            }
        }
        return bufferedImage.getSubimage(0, 0, width, height)
    }

    fun roundUp(groupSize:Int, globalSize:Int): Int{
        var r = globalSize % groupSize
        if(r == 0){
            return globalSize
        }else{
            return globalSize + groupSize - r
        }
    }

    fun setSize(width:Int, height:Int){
        this.width=width
        this.height=height
    }
}
