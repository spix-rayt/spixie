package spixie.render

import com.jogamp.opencl.CLBuffer
import java.nio.FloatBuffer

class ImageCLBuffer(val buffer: CLBuffer<FloatBuffer>, val width: Int, val height: Int)