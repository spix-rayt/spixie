package spixie.renderer


import com.aparapi.device.OpenCLDevice
import com.aparapi.internal.kernel.KernelManager
import spixie.static.roundUp
import java.nio.FloatBuffer

class AparapiRenderer: Renderer {
    private var width = 256
    private var height = 256
    private var realWidth = 1
    private var realHeight = 1
    private val device = (KernelManager.instance().bestDevice().apply { println(this) } as OpenCLDevice)

    private lateinit var kernel: AparapiKernelInterface

    init {
        TODO("Dont work. It will probably be removed.")
    }

    override fun render(particlesArray: FloatBuffer, depth: Boolean): FloatArray {
        val particlesCount = (particlesArray.capacity()/ RenderBufferBuilder.PARTICLE_FLOAT_SIZE).roundUp(256)
        if(particlesCount == 0){
            return FloatArray(realWidth*realHeight*4)
        }
        val imageOut = FloatArray(realWidth*realHeight*4)
        val clParticles = particlesArray.array()

        //app getting stuck with this code :( idk why
        /*if(!::kernel.isInitialized){
            kernel = device.bind(AparapiKernelInterface::class.java)
        }
        kernel.renderParticles(device.createRange(width*height, 256), clParticles, width, height, realWidth, particlesCount, imageOut)*/

        kernel = device.bind(AparapiKernelInterface::class.java)
        kernel.renderParticles(device.createRange(width*height, 256), clParticles, width, height, realWidth, particlesCount, 4, 0, imageOut)
        kernel.dispose()

        return imageOut
    }

    override fun setSize(width: Int, height: Int) {
        this.width=width.roundUp(256)
        this.height=height
        this.realWidth = width
        this.realHeight = height
    }

    override fun getSize(): Pair<Int, Int> {
        return realWidth to realHeight
    }
}