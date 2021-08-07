package spixie.visualEditor

import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.stage.Screen
import spixie.WorkWindow
import spixie.visualEditor.components.ImageResult
import spixie.visualEditor.components.MoveRotate
import spixie.visualEditor.components.Render

const val VE_GRID_CELL_SIZE = 20.0
const val VE_PIN_WIDTH = VE_GRID_CELL_SIZE * 5
const val VE_KEK = VE_GRID_CELL_SIZE * 4

class VisualEditor: BorderPane(), WorkWindow.OpenableContent {
    var mainModule = Module()
        private set

    val modules = arrayListOf(mainModule)

    var time = 0.0
        private set

    var downscale = 1
        private set

    init {
        setOnMouseClicked { event ->
            if(event.button == MouseButton.PRIMARY){
                requestFocus()
                event.consume()
            }
        }

        setOnKeyPressed { event ->
            if(event.code == KeyCode.HOME){
                homeLayout()
                event.consume()
            }
            if(event.code == KeyCode.DELETE) {
                mainModule.selectedComponents.forEach {
                    it.deleteComponent()
                }
                mainModule.selectedComponents = emptyArray()
            }
        }

        mainModule.apply {
            val resultComponent = ImageResult()
            addComponent(resultComponent)
            val renderComponent = Render()
            addComponent(renderComponent)
            resultComponent.inputPins[0].connectWith(renderComponent.outputPins[0])

            val moveComponent = MoveRotate()
            addComponent(moveComponent)
            moveComponent.changeZ(1000.0)
            renderComponent.inputPins[0].connectWith(moveComponent.outputPins[0])

            renderComponent.magneticRelocate(moveComponent.layoutX + moveComponent.width + VE_GRID_CELL_SIZE, moveComponent.layoutY + moveComponent.height - renderComponent.height)
            resultComponent.magneticRelocate(renderComponent.layoutX+renderComponent.width + VE_GRID_CELL_SIZE, renderComponent.layoutY+renderComponent.height-resultComponent.height)
        }

        loadModule(mainModule)
    }

    fun loadModule(module: Module){
        this.mainModule = module
        center = module.contentPane
        homeLayout()
    }

    fun render(time:Double, downscale: Int): ImageFloatBuffer {
        this.time = time
        this.downscale = downscale
        return mainModule.findResultComponent().getImage()
    }

    private fun homeLayout(){
        val visualBounds = Screen.getPrimary().visualBounds
        val resultComponent = mainModule.findResultComponentNode()
        mainModule.content.layoutX = visualBounds.width/2 - resultComponent.layoutX
        mainModule.content.layoutY = visualBounds.height/2 - resultComponent.layoutY
    }
}