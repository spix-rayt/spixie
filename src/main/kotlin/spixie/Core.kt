package spixie

import spixie.arrangement.ArrangementWindow
import spixie.opencl.OpenCLApi

object Core {
    val renderManager by lazy { RenderManager() }
    val workWindow by lazy { WorkWindow() }
    val arrangementWindow by lazy { ArrangementWindow() }
    val audio by lazy { Audio() }
    val opencl by lazy { OpenCLApi() }

    var dragAndDropObject: Any = Any()
}