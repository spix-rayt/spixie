package spixie

import com.jogamp.opencl.*
import spixie.static.roundUp
import java.awt.image.BufferedImage
import java.nio.FloatBuffer
import java.nio.IntBuffer

class OpenCLRenderer {
    val context:CLContext
    val device:CLDevice
    val program:CLProgram
    var clImageOut:CLBuffer<IntBuffer>
    var width = 1
    var height = 1
    var realWidth = 1
    var realHeight = 1

    init {
        context = CLContext.create()
        device = context.maxFlopsDevice
        program = context.createProgram(javaClass.getResourceAsStream("/kernel.cl")).build()
        clImageOut = context.createIntBuffer(width*height*4, CLMemory.Mem.WRITE_ONLY)
    }

    fun render(particlesArray:FloatBuffer): BufferedImage {
        val queue = device.createCommandQueue()
        val localWorkSize = 256
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
        val raster = bufferedImage.raster

        val particlesCount = (particlesArray.capacity()/RenderBufferBuilder.PARTICLE_FLOAT_SIZE).roundUp(256)
        if(particlesCount == 0){
            return bufferedImage
        }

        val clParticles = context.createFloatBuffer(particlesCount*RenderBufferBuilder.PARTICLE_FLOAT_SIZE, CLMemory.Mem.READ_ONLY)


        clParticles.buffer.put(particlesArray)
        clParticles.buffer.rewind()
        clImageOut.buffer.rewind()

        val kernel = program.createCLKernel("RenderParticles")
        kernel.putArgs(clParticles)
        kernel.putArg(width)
        kernel.putArg(height)
        kernel.putArg(realWidth)
        kernel.putArg(particlesCount)
        kernel.putArgs(clImageOut)

        queue.putWriteBuffer(clParticles, false)
                .put1DRangeKernel(kernel, 0L, (width * height).toLong(), localWorkSize.toLong())
                .putReadBuffer(clImageOut, true)


        val intArray = IntArray(width*height*4)
        clImageOut.buffer.get(intArray)
        raster.setPixels(0, 0, width, height, intArray)
        bufferedImage.data = raster
        clParticles.release()
        return bufferedImage.getSubimage(0, 0, realWidth, realHeight)
    }

    fun setSize(width:Int, height:Int){
        this.width=width.roundUp(256)
        this.height=height
        this.realWidth = width
        this.realHeight = height
        clImageOut.release()
        clImageOut = context.createIntBuffer(this.width*this.height*4, CLMemory.Mem.WRITE_ONLY)
    }
}
