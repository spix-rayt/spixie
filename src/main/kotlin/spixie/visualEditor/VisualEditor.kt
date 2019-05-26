package spixie.visualEditor

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.subjects.PublishSubject
import javafx.scene.control.ListView
import javafx.scene.effect.DropShadow
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Screen
import spixie.Core
import spixie.Main
import spixie.WorkingWindow
import spixie.visualEditor.components.ImageResult
import spixie.visualEditor.components.MoveRotate
import spixie.visualEditor.components.ParticlesResult
import spixie.visualEditor.components.Render
import java.lang.Exception

const val VE_GRID_CELL_SIZE = 20.0
const val VE_PIN_WIDTH = VE_GRID_CELL_SIZE * 5
const val VE_KEK = VE_GRID_CELL_SIZE*4

class VisualEditor: BorderPane(), WorkingWindow.OpenableContent {
    var currentModule = Module("Main")
        private set

    val modules = arrayListOf(currentModule)

    var time = 0.0
        private set

    var downscale = 1
        private set

    init {
        val shiftKeyReleaseEvents = PublishSubject.create<Unit>()

        setOnMouseClicked { event ->
            if(event.button == MouseButton.PRIMARY){
                requestFocus()
                event.consume()
            }
        }

        setOnKeyReleased { event ->
            if(event.code == KeyCode.HOME){
                homeLayout()
                event.consume()
            }
            if(event.code == KeyCode.INSERT){
                val newModule = Module("m${modules.size}")
                modules.add(newModule)
                newModule.addComponent(ParticlesResult().apply { creationInit(); configInit() })
                loadModule(newModule)
                event.consume()
            }
            if(event.code == KeyCode.SHIFT){
                shiftKeyReleaseEvents.onNext(Unit)
            }
            if(event.code == KeyCode.E){
                if(!currentModule.isMain){
                    ModuleSettingsDialog(this.scene.window, currentModule)
                }
            }
        }

        shiftKeyReleaseEvents
                .timeInterval()
                .filter { it.time()<300 }
                .observeOn(JavaFxScheduler.platform())
                .subscribe {
                    val root = scene.root as StackPane
                    if(root.children.find { it is ListView<*> } == null){
                        val listView = ListView<Any>().apply {
                            Core.arrangementWindow.visualEditor.modules.forEach { this.items.add(it) }
                            maxWidth = 250.0
                            maxHeight = 450.0
                        }
                        root.children.addAll(listView)
                        listView.effect = DropShadow(20.0, 4.0, 4.0, Color.GRAY)
                        listView.layoutBoundsProperty().addListener { _, _, newValue ->
                            listView.relocate(Screen.getPrimary().bounds.width/2 - newValue.width/2, Screen.getPrimary().bounds.height/2 - newValue.height/2)
                        }
                        listView.focusedProperty().addListener { _, _, newValue ->
                            if(!newValue){
                                root.children.remove(listView)
                            }
                        }
                        listView.selectionModel.select(Core.arrangementWindow.visualEditor.currentModule)
                        listView.requestFocus()
                        listView.setOnKeyPressed { event ->
                            if(event.code == KeyCode.ENTER){
                                (listView.selectionModel.selectedItem as? Module)?.let {
                                    Core.arrangementWindow.visualEditor.loadModule(it)
                                    listView.toBack()
                                    root.children.remove(listView)
                                }
                            }
                            if(event.code == KeyCode.ESCAPE){
                                listView.toBack()
                                root.children.remove(listView)
                            }
                        }
                    }
                }

        currentModule.apply {
            val resultComponent = ImageResult().apply {
                creationInit()
                configInit()
            }
            addComponent(resultComponent)
            val renderComponent = Render().apply {
                creationInit()
                configInit()
            }
            addComponent(renderComponent)
            resultComponent.inputPins[0].connectWith(renderComponent.outputPins[0])

            val moveComponent = MoveRotate().apply {
                creationInit()
                configInit()
            }
            addComponent(moveComponent)
            moveComponent.changeZ(1000.0)
            renderComponent.inputPins[0].connectWith(moveComponent.outputPins[0])

            renderComponent.magneticRelocate(moveComponent.layoutX + moveComponent.width + VE_GRID_CELL_SIZE, moveComponent.layoutY + moveComponent.height - renderComponent.height)
            resultComponent.magneticRelocate(renderComponent.layoutX+renderComponent.width + VE_GRID_CELL_SIZE, renderComponent.layoutY+renderComponent.height-resultComponent.height)
        }

        loadModule(currentModule)
    }

    fun loadModule(module: Module){
        this.currentModule = module
        center = module.contentPane
        homeLayout()
    }

    fun render(time:Double, downscale: Int): ImageFloatBuffer {
        this.time = time
        this.downscale = downscale
        val mainModule = modules.find { it.isMain } ?: throw Exception("Main module does not exist")
        return mainModule.findResultComponent().getImage()
    }

    private fun homeLayout(){
        val visualBounds = Screen.getPrimary().visualBounds
        val resultComponent = currentModule.findResultComponentNode()
        currentModule.content.layoutX = visualBounds.width/2 - resultComponent.layoutX
        currentModule.content.layoutY = visualBounds.height/2 - resultComponent.layoutY
    }
}