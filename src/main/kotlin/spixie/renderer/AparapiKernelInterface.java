package spixie.renderer;

import com.aparapi.Range;
import com.aparapi.opencl.OpenCL;

@OpenCL.Resource("kernel.cl")
public interface AparapiKernelInterface extends OpenCL<AparapiKernelInterface> {
    public AparapiKernelInterface renderParticles(Range _range, @GlobalReadOnly("particles") float[] particles, @Arg("width") int width, @Arg("height") int height, @Arg("realWidth") int realWidth, @Arg("particlesCount") int particlesCount, @GlobalWriteOnly("outImage") float[] outImage);
}
