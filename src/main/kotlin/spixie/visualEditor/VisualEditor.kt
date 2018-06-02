package spixie.visualEditor

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.subjects.PublishSubject
import javafx.scene.Group
import javafx.scene.control.ListView
import javafx.scene.effect.DropShadow
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.stage.Screen
import spixie.Main
import spixie.WorkingWindowOpenableContent
import spixie.visualEditor.components.MoveRotate
import spixie.visualEditor.components.Render
import spixie.visualEditor.components.Result

class VisualEditor: BorderPane(), WorkingWindowOpenableContent {
    var currentModule = Module("Main")
        private set
    val modules = arrayListOf(currentModule)
    var time = 0.0
    private set
    var downscale = 1
    private set

    init {
        loadModule(currentModule)

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
                loadModule(newModule)
                event.consume()
            }
            if(event.code == KeyCode.SHIFT){
                shiftKeyReleaseEvents.onNext(Unit)
            }
        }

        shiftKeyReleaseEvents
                .timeInterval()
                .filter { it.time()<300 }
                .observeOn(JavaFxScheduler.platform())
                .subscribe {
                    val root = scene.root as Group
                    if(root.children.find { it is ListView<*> } == null){
                        val listView = ListView<Any>().apply { Main.arrangementWindow.visualEditor.modules.forEach { this.items.add(it) } }
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
                        listView.selectionModel.select(Main.arrangementWindow.visualEditor.currentModule)
                        listView.requestFocus()
                        listView.setOnKeyPressed { event ->
                            if(event.code == KeyCode.ENTER){
                                (listView.selectionModel.selectedItem as? Module)?.let {
                                    Main.arrangementWindow.visualEditor.loadModule(it)
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
            val resultComponent = Result()
            addComponent(resultComponent)
            resultComponent.magneticRelocate(768.0, 0.0)

            val renderComponent = Render()
            addComponent(renderComponent)
            renderComponent.magneticRelocate(384.0, 0.0)
            resultComponent.inputPins[0].connectWith(renderComponent.outputPins[0])

            val moveComponent = MoveRotate()
            addComponent(moveComponent)
            moveComponent.changeZ(1000.0)
            renderComponent.inputPins[0].connectWith(moveComponent.outputPins[0])
        }
    }

    fun loadModule(module: Module){
        this.currentModule = module
        center = module.contentPane
        homeLayout()
    }

    fun render(time:Double, downscale: Int): ImageFloatArray {
        this.time = time
        this.downscale = downscale
        return modules.find { it.isMain }?.findResultComponent()?.getImage() ?: ImageFloatArray(floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f), 1, 1)
    }

    private fun homeLayout(){
        val visualBounds = Screen.getPrimary().visualBounds
        currentModule.content.layoutX = visualBounds.width/2
        currentModule.content.layoutY = visualBounds.height/2
    }
}