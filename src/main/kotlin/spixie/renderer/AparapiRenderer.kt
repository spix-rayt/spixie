package spixie.renderer


import com.aparapi.device.OpenCLDevice
import com.aparapi.internal.kernel.KernelManager
import spixie.static.roundUp
import java.awt.image.BufferedImage
import java.nio.FloatBuffer

class AparapiRenderer: Renderer {
    private var width = 256
    private var height = 256
    private var realWidth = 1
    private var realHeight = 1
    private val device = (KernelManager.instance().bestDevice().apply { println(this) } as OpenCLDevice)

    override fun render(particlesArray: FloatBuffer): BufferedImage {
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
        val raster = bufferedImage.raster

        val particlesCount = (particlesArray.capacity()/ RenderBufferBuilder.PARTICLE_FLOAT_SIZE).roundUp(256)
        if(particlesCount == 0){
            return bufferedImage.getSubimage(0, 0, realWidth, realHeight)
        }

        val clParticles = particlesArray.array()
        val imageOut = IntArray(width*height*4)
        val kernel = device.bind(AparapiKernelInterface::class.java)
        kernel.renderParticles(device.createRange(width*height, 256), clParticles, width, height, realWidth, particlesCount, imageOut)
        kernel.dispose()

        raster.setPixels(0, 0, width, height, imageOut)
        bufferedImage.data = raster
        return bufferedImage.getSubimage(0, 0, realWidth, realHeight)
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