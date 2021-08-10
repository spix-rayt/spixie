package spixie.visualEditor

import io.reactivex.subjects.PublishSubject
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import spixie.timelineWindow
import spixie.visualEditor
import spixie.visualEditor.pins.ComponentPin
import kotlin.math.floor
import kotlin.math.roundToInt

abstract class EditorComponent: Region() {
    val parameters = arrayListOf<Node>()

    val inputPins = arrayListOf<ComponentPin>()

    val outputPins = arrayListOf<ComponentPin>()

    val content = Group()

    private var dragDelta = Point2D(0.0, 0.0)

    val conneectionsChanged = PublishSubject.create<Unit>()

    val disconnectPinRequest = PublishSubject.create<ComponentPin>()

    val relocateSelectedRequests = PublishSubject.create<Pair<Double, Double>>()

    var selected = false
        set(value) {
            field=value
            border = if(value){
                Border(BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(2.0)))
            }else{
                Border(BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(1.0)))
            }
        }

    init {
        selected = false
        style = "-fx-background-color: #FFFFFFFF;"

        setOnMouseClicked { event ->
            event.consume()
        }

        setOnMousePressed { event ->
            if(event.button == MouseButton.PRIMARY){
                dragDelta = Point2D(layoutX - event.sceneX, layoutY - event.sceneY)
            }
        }

        setOnMouseDragged { event ->
            if(event.button == MouseButton.PRIMARY){
                val newX = ((event.sceneX + dragDelta.x) / VE_GRID_CELL_SIZE).roundToInt() * VE_GRID_CELL_SIZE
                val newY = floor((event.sceneY + dragDelta.y) / VE_GRID_CELL_SIZE) * VE_GRID_CELL_SIZE
                if(layoutX != newX || layoutY != newY){
                    val diffX = newX - layoutX
                    val diffY = newY - layoutY
                    relocateSelectedRequests.onNext(diffX to diffY)
                }
                event.consume()
            }
        }

        children.addAll(content)
    }

    fun deleteComponent() {
        visualEditor.mainModule.removeComponent(this)
        this@EditorComponent.inputPins.forEach { disconnectPinRequest.onNext(it) }
        this@EditorComponent.outputPins.forEach { disconnectPinRequest.onNext(it) }
    }

    fun magneticRelocate(x: Double, y:Double){
        val newX = (x / VE_GRID_CELL_SIZE).roundToInt() * VE_GRID_CELL_SIZE
        //val newY = floor(y / VE_GRID_CELL_SIZE) * VE_GRID_CELL_SIZE
        val newY = (y / VE_GRID_CELL_SIZE).roundToInt() * VE_GRID_CELL_SIZE
        if(layoutX != newX || layoutY != newY){
            relocate(newX, newY)
            conneectionsChanged.onNext(Unit)
        }
    }

    fun relativelyMagneticRelocate(x: Double, y:Double){
        magneticRelocate(layoutX + x, layoutY + y)
    }

    fun getReadableName(): String {
        return javaClass.simpleName.replace(Regex("[A-Z]"), { matchResult -> " ${matchResult.value}" })
    }

    fun updateUI(){
        content.children.clear()
        content.children.addAll(
                Label(getReadableName()).apply {
                    style="-fx-font-weight: bold; -fx-font-style: italic;"
                    alignment = Pos.CENTER
                    prefWidth = VE_PIN_WIDTH + VE_KEK + 1.0
                    prefHeight = VE_GRID_CELL_SIZE
                }
        )
        content.children.addAll(parameters)
        content.children.addAll(inputPins)
        content.children.addAll(outputPins)

        parameters.forEachIndexed { index, node ->
            node.apply {
                this.layoutX = 0.0
                this.layoutY = index*VE_GRID_CELL_SIZE+VE_GRID_CELL_SIZE
                this.minWidth(VE_PIN_WIDTH - VE_GRID_CELL_SIZE)
                this.maxWidth(VE_PIN_WIDTH - VE_GRID_CELL_SIZE)

                this.minHeight(VE_GRID_CELL_SIZE)
                this.maxHeight(VE_GRID_CELL_SIZE)
            }
        }

        val visibleInputPins = inputPins.filter { it.isVisible }

        visibleInputPins.forEachIndexed { index, pin ->
            pin.layoutX = 0.0
            pin.layoutY = index*VE_GRID_CELL_SIZE+VE_GRID_CELL_SIZE + parameters.size*VE_GRID_CELL_SIZE
            pin.updateUI()
        }

        outputPins.forEachIndexed { index, pin ->
            pin.layoutX = VE_KEK
            pin.layoutY = index*VE_GRID_CELL_SIZE+VE_GRID_CELL_SIZE + visibleInputPins.size*VE_GRID_CELL_SIZE + parameters.size*VE_GRID_CELL_SIZE
            pin.updateUI()
        }


        prefWidth = VE_PIN_WIDTH + VE_KEK + 1.0
        prefHeight = getHeightInCells() * VE_GRID_CELL_SIZE + 1.0
        width = VE_PIN_WIDTH + VE_KEK + 1.0
        height = getHeightInCells() * VE_GRID_CELL_SIZE + 1.0
        conneectionsChanged.onNext(Unit)
    }

    fun findInputPinByName(pinName: String): ComponentPin? {
        return inputPins.find { it.name == pinName }
    }

    fun findOutputPinByName(pinName: String): ComponentPin? {
        return outputPins.find { it.name == pinName }
    }

    protected open fun getHeightInCells(): Int {
        val visibleInputPins = inputPins.filter { it.isVisible }
        return visibleInputPins.size + parameters.size + outputPins.size + 1
    }
}