package spixie.visualEditor

import io.reactivex.subjects.PublishSubject
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import spixie.Main
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

abstract class Component:Region(), Externalizable {
    val parameters = arrayListOf<Node>()
    val inputPins = arrayListOf<ComponentPin<*>>()
    val outputPins = arrayListOf<ComponentPin<*>>()
    val content = Group()

    private var dragDelta = Point2D(0.0, 0.0)


    val conneectionsChanged = PublishSubject.create<Unit>()
    val relocations = PublishSubject.create<Unit>()
    val disconnectPinRequest = PublishSubject.create<ComponentPin<*>>()
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
                val newX = ((event.sceneX + dragDelta.x) / 32.0).roundToInt() * 32.0
                val newY = floor((event.sceneY + dragDelta.y) / 32.0) * 32.0
                if(layoutX != newX || layoutY != newY){
                    val diffX = newX - layoutX
                    val diffY = newY - layoutY
                    relocateSelectedRequests.onNext(diffX to diffY)
                }
                event.consume()
            }
        }

        setOnContextMenuRequested { event->
            ContextMenu(
                    MenuItem("Delete").apply {
                        setOnAction {
                            Main.arrangementWindow.visualEditor.modules.forEach {
                                it.removeComponent(this@Component)
                            }
                            this@Component.inputPins.forEach { disconnectPinRequest.onNext(it) }
                            this@Component.outputPins.forEach { disconnectPinRequest.onNext(it) }
                        }
                    }
            ).show(this, event.screenX, event.screenY)
            event.consume()
        }

        children.addAll(content)
    }

    fun magneticRelocate(x: Double, y:Double){
        val newX = (x / 32.0).roundToInt() * 32.0
        val newY = floor(y / 32.0) * 32.0
        if(layoutX != newX || layoutY != newY){
            relocate(newX, newY)
            relocations.onNext(Unit)
        }
    }

    fun relativelyMagneticRelocate(x: Double, y:Double){
        magneticRelocate(layoutX + x, layoutY + y)
    }

    fun updateVisual(){
        content.children.clear()
        content.children.addAll(
                Label(javaClass.simpleName.replace(Regex("[A-Z]"), { matchResult -> " ${matchResult.value}" })).apply {
                    style="-fx-font-weight: bold; -fx-font-style: italic;"
                    alignment = Pos.CENTER
                    prefWidth = 256.0 + 96.0 + 1.0
                    prefHeight = 32.0
                }
        )
        content.children.addAll(parameters)
        content.children.addAll(inputPins)
        content.children.addAll(outputPins)

        parameters.forEachIndexed { index, node ->
            node.apply {
                this.layoutX = 0.0
                this.layoutY = index*32.0+32.0
                this.minWidth(128.0 - 32.0)
                this.maxWidth(128.0 - 32.0)

                this.minHeight(32.0)
                this.maxHeight(32.0)
            }
        }

        val visibleInputPins = inputPins.filter { it.isVisible }

        visibleInputPins.forEachIndexed { index, pin ->
            pin.layoutX = 0.0
            pin.layoutY = index*32.0+32.0 + parameters.size*32.0
            pin.updateUI()
        }

        outputPins.forEachIndexed { index, pin ->
            pin.layoutX = 256.0 + 96.0 - 128.0
            pin.layoutY = index*32.0+32.0
            pin.updateUI()
        }


        prefWidth = 256.0 + 96.0 + 1.0
        prefHeight = max(visibleInputPins.size + parameters.size, outputPins.size)*32.0 + 1.0 + 32.0
        width = 256.0 + 96.0 + 1.0
        height = max(visibleInputPins.size + parameters.size, outputPins.size)*32.0 + 1.0 + 32.0
    }

    var serializationIndex = -1

    override fun writeExternal(o: ObjectOutput) {
        o.writeDouble(layoutX)
        o.writeDouble(layoutY)
        o.writeObject(inputPins.map { it.valueControl?.value })
        o.writeInt(serializationIndex)
    }

    override fun readExternal(o: ObjectInput) {
        magneticRelocate(o.readDouble(), o.readDouble())
        val inputPinsValues = o.readObject() as List<Double?>
        inputPinsValues.zip(inputPins).forEach {
            it.first?.let { v ->
                it.second.valueControl?.value = v
            }
        }
        serializationIndex = o.readInt()
    }

    companion object {
        const val serialVersionUID = 0L
    }
}