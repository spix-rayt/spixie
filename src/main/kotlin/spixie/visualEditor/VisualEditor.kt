package spixie.visualEditor

import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.stage.Screen
import spixie.ProjectWindow
import spixie.render.ImageCLBuffer
import spixie.visualEditor.components.RenderComponent

const val VE_GRID_CELL_SIZE = 20.0
const val VE_PIN_WIDTH = VE_GRID_CELL_SIZE * 5
const val VE_KEK = VE_GRID_CELL_SIZE * 4

class VisualEditor: BorderPane(), ProjectWindow.OpenableContent {
    var mainModule = Module()
        private set

    val modules = arrayListOf(mainModule)

    var beats = 0.0
        private set

    init {
        setOnMouseClicked { event ->
            if(event.button == MouseButton.PRIMARY) {
                requestFocus()
                event.consume()
            }
        }

        setOnKeyPressed { event ->
            if(event.code == KeyCode.HOME) {
                homeLayout()
                event.consume()
            }
            if(event.code == KeyCode.DELETE) {
                mainModule.selectedComponents.forEach {
                    mainModule.removeComponent(it)
                }
                mainModule.selectedComponents = emptyArray()
            }
        }

        mainModule.apply {
            val renderComponent = RenderComponent()
            addComponent(renderComponent)

            relocateAllComponents()
        }

        setAsMainModule(mainModule)
    }

    fun setAsMainModule(module: Module) {
        this.mainModule = module
        center = module.contentPane
        homeLayout()
    }

    fun render(imageCLBuffer: ImageCLBuffer, beats: Double, samplesPerPixel: Int) {
        this.beats = beats
        mainModule.findRenderComponent().invoke(imageCLBuffer, samplesPerPixel)
    }

    private fun homeLayout() {
        val visualBounds = Screen.getPrimary().visualBounds
        val renderComponent = mainModule.findRenderComponentNode()
        mainModule.content.layoutX = visualBounds.width/2 - renderComponent.layoutX
        mainModule.content.layoutY = visualBounds.height/2 - renderComponent.layoutY
    }
}