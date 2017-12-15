package spixie.components

import javafx.application.Platform
import javafx.scene.input.ClipboardContent
import javafx.scene.input.MouseButton
import javafx.scene.input.TransferMode
import spixie.*

class Graph(x:Double, y:Double) : VisualEditorComponent(x, y), SpixieHashable {
    val graphWindow: GraphWindow = GraphWindow()
    init{
        minWidth = 50.0
        minHeight = 25.0

        setOnContextMenuRequested { event ->
            Platform.runLater {
                Main.workingWindow.nextOpen(graphWindow)
            }
            event.consume()
        }

        setOnDragOver { event ->
            if(event.dragboard.hasContent(DragAndDropType.INTERNALOBJECT)){
                event.acceptTransferModes(TransferMode.LINK)
            }
            event.consume()
        }

        setOnDragDropped { event ->
            val dragboard = event.dragboard
            var success = false
            if(dragboard.hasContent(DragAndDropType.INTERNALOBJECT)){
                onValueInputOutputConnected(Main.internalObject, this)
                val obj = Main.internalObject
                if(obj is ValueControl){
                    obj.value.input = graphWindow.value
                }
                success = true
            }
            event.isDropCompleted = success
            event.consume()
        }

        setOnDragDetected { event ->
            if(event.button == MouseButton.PRIMARY){
                if(event.isControlDown){
                    val startDragAndDrop = this.startDragAndDrop(javafx.scene.input.TransferMode.LINK);
                    val content = ClipboardContent()
                    content.put(DragAndDropType.INTERNALOBJECT, "void")
                    startDragAndDrop.setContent(content)
                    Main.internalObject = this
                    event.consume()
                }
            }
        }
    }

    var onValueInputOutputConnected: (Any, Any) -> Unit = { _, _ ->  }
}