package spixie.renderer

import com.jogamp.opencl.*
import spixie.static.roundUp
import java.nio.FloatBuffer

class JOCLRenderer: Renderer {
    private val context:CLContext = CLContext.create()
    private val device:CLDevice
    private val program:CLProgram
    private var clImageOut:CLBuffer<FloatBuffer>
    private var width = 1
    private var height = 1
    private var realWidth = 1
    private var realHeight = 1

    init {
        device = context.maxFlopsDevice
        program = context.createProgram(javaClass.getResourceAsStream("/kernel.cl")).build()
        clImageOut = context.createFloatBuffer(realWidth*realHeight*4, CLMemory.Mem.WRITE_ONLY)
    }

    override fun render(particlesArray:FloatBuffer, depth: Boolean): FloatArray {
        val queue = device.createCommandQueue()
        val localWorkSize = 64

        val particlesCount = (particlesArray.capacity()/ RenderBufferBuilder.PARTICLE_FLOAT_SIZE).roundUp(64)
        if(particlesCount == 0){
            return FloatArray(realWidth*realHeight*4)
        }

        val clParticles = context.createFloatBuffer(particlesCount * RenderBufferBuilder.PARTICLE_FLOAT_SIZE, CLMemory.Mem.READ_ONLY)


        clParticles.buffer.put(particlesArray)
        clParticles.buffer.rewind()
        clImageOut.buffer.rewind()

        val kernel = program.createCLKernel("renderParticles")
        kernel.putArgs(clParticles)
        kernel.putArg(width)
        kernel.putArg(height)
        kernel.putArg(realWidth)
        kernel.putArg(particlesCount)
        kernel.putArg(if(depth) 1 else 0)
        kernel.putArgs(clImageOut)

        queue.putWriteBuffer(clParticles, false)
                .put1DRangeKernel(kernel, 0L, (width * height).toLong(), localWorkSize.toLong())
                .putReadBuffer(clImageOut, true)


        val floatArray = FloatArray(realWidth*realHeight*4)
        clImageOut.buffer.get(floatArray)
        clParticles.release()
        return floatArray
    }

    override fun setSize(width:Int, height:Int){
        this.width=width.roundUp(64)
        this.height=height
        this.realWidth = width
        this.realHeight = height
        clImageOut.release()
        clImageOut = context.createFloatBuffer(this.realWidth*this.realHeight*4, CLMemory.Mem.WRITE_ONLY)
    }

    override fun getSize(): Pair<Int, Int> {
        return realWidth to realHeight
    }
}
