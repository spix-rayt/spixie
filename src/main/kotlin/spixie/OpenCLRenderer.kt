package spixie

import com.jogamp.opencl.*
import java.awt.image.BufferedImage
import java.nio.FloatBuffer

class OpenCLRenderer {
    var context:CLContext? = null
    var device:CLDevice? = null
    var program:CLProgram? = null
    var clImageOut:CLBuffer<FloatBuffer>? = null
    var width = 1
    var height = 1

    init {
        context = CLContext.create()
        device = context!!.getMaxFlopsDevice()
        program = context!!.createProgram(javaClass.getResourceAsStream("/kernel.cl")).build()
        clImageOut = context!!.createFloatBuffer(width*height*4, CLMemory.Mem.WRITE_ONLY)
    }

    fun render(particlesArray:FloatArray): BufferedImage {
        val queue = device!!.createCommandQueue()

        val PARTICLE_SIZE=RenderBufferBuilder.PARTICLE_FLOAT_SIZE
        val PARTICLES=particlesArray.size/PARTICLE_SIZE
        if(PARTICLES==0){
            return BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
        }
        val localWorkSize = minOf(device!!.getMaxWorkGroupSize(), 256)


        val clParticles = context!!.createFloatBuffer(PARTICLES*PARTICLE_SIZE, CLMemory.Mem.READ_ONLY)


        clParticles.buffer.put(particlesArray)
        clParticles.buffer.rewind()
        clImageOut!!.buffer.rewind()

        val kernel = program!!.createCLKernel("RenderParticles")
        kernel.putArgs(clParticles)
        kernel.putArg(width)
        kernel.putArg(height)
        kernel.putArg(PARTICLES)
        kernel.putArgs(clImageOut)

        queue.putWriteBuffer(clParticles, false)
                .put1DRangeKernel(kernel, 0L, roundUp(localWorkSize, width * height).toLong(), localWorkSize.toLong())
                .putReadBuffer(clImageOut, true)

        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
        val raster = bufferedImage.raster

        val intArray = IntArray(width*height*4)
        for(i in 0..width*height*4-1){
            val round = Math.round(clImageOut!!.buffer.get() * 255)
            intArray[i] = Math.max(0, Math.min(round, 255))
        }
        raster.setPixels(0,0, width, height, intArray)
        bufferedImage.data = raster
        clParticles.release()
        return bufferedImage
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
        clImageOut!!.release()
        clImageOut = context!!.createFloatBuffer(width*height*4, CLMemory.Mem.WRITE_ONLY)
    }
}
