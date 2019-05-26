package spixie

import spixie.arrangement.ArrangementWindow
import spixie.opencl.OpenCLApi

object Core {
    var renderManager = RenderManager()
    val workingWindow = WorkingWindow()
    val arrangementWindow = ArrangementWindow()
    val audio = Audio()
    val opencl = OpenCLApi()

    var dragAndDropObject: Any = Any()
}