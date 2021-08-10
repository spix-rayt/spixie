package spixie.visualEditor

import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.stage.Screen
import spixie.WorkWindow
import spixie.render.ImageCLBuffer
import spixie.visualEditor.components.MoveRotate
import spixie.visualEditor.components.RenderComponent

const val VE_GRID_CELL_SIZE = 20.0
const val VE_PIN_WIDTH = VE_GRID_CELL_SIZE * 5
const val VE_KEK = VE_GRID_CELL_SIZE * 4

class VisualEditor: BorderPane(), WorkWindow.OpenableContent {
    var mainModule = Module()
        private set

    val modules = arrayListOf(mainModule)

    var beats = 0.0
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
            val renderComponent = RenderComponent()
            addComponent(renderComponent)

            val moveComponent = MoveRotate()
            addComponent(moveComponent)
            moveComponent.changeZ(20.0)
            renderComponent.inputPins[0].connectWith(moveComponent.outputPins[0])

            renderComponent.magneticRelocate(moveComponent.layoutX + moveComponent.width + VE_GRID_CELL_SIZE, moveComponent.layoutY + moveComponent.height - renderComponent.height)
        }

        loadModule(mainModule)
    }

    fun loadModule(module: Module){
        this.mainModule = module
        center = module.contentPane
        homeLayout()
    }

    fun render(imageCLBuffer: ImageCLBuffer, beats: Double, samplesPerPixel: Int) {
        this.beats = beats
        mainModule.findRenderComponent().invoke(imageCLBuffer, samplesPerPixel)
    }

    private fun homeLayout(){
        val visualBounds = Screen.getPrimary().visualBounds
        val resultComponent = mainModule.findFinalComponentNode()
        mainModule.content.layoutX = visualBounds.width/2 - resultComponent.layoutX
        mainModule.content.layoutY = visualBounds.height/2 - resultComponent.layoutY
    }
}