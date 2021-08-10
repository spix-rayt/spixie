package spixie

import spixie.render.OpenCLApi
import spixie.timeline.TimelineWindow
import spixie.visualEditor.VisualEditor
import java.awt.Robot

val openCLApi by lazy { OpenCLApi() }
val visualEditor by lazy { VisualEditor() }
val audio by lazy { Audio() }
val renderManager by lazy { RenderManager() }
val timelineWindow by lazy { TimelineWindow() }
val workWindow by lazy { WorkWindow() }
val robot by lazy { Robot() }
var dragAndDropObject: Any = Any()