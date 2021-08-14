package spixie

import com.google.gson.Gson
import javafx.scene.input.DataFormat
import spixie.render.OpenCLApi
import spixie.timeline.TimelineWindow
import spixie.visualEditor.VisualEditor
import java.awt.Robot

val projectWindow = ProjectWindow()
val openCLApi = OpenCLApi()
val renderManager = RenderManager()
val visualEditor = VisualEditor()
val audio = Audio()
val timelineWindow = TimelineWindow()
val robot = Robot()
var dragAndDropObject: Any = Any()
val gson = Gson()

object DragAndDropType {
    val PIN = DataFormat("PIN")
}