package spixie.visualEditor

import io.reactivex.subjects.PublishSubject
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.scene.layout.Region
import spixie.Main
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

open class Component:Region(), Externalizable {
    val inputPins = arrayListOf<ComponentPin<*>>()
    val outputPins = arrayListOf<ComponentPin<*>>()
    val content = Group()

    private var dragDelta = Point2D(0.0, 0.0)


    val conneectionsChanged = PublishSubject.create<Unit>()
    val relocations = PublishSubject.create<Unit>()
    val disconnectPinRequest = PublishSubject.create<ComponentPin<*>>()

    init {
        style = "-fx-border-color: #000000FF; -fx-border-width: 1; -fx-background-color: #FFFFFFFF;"

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
                magneticRelocate(event.sceneX + dragDelta.x, event.sceneY + dragDelta.y)
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

    fun updateVisual(){
        content.children.clear()
        content.children.addAll(inputPins)
        content.children.addAll(outputPins)
        inputPins.forEachIndexed { index, pin ->
            pin.layoutX = 0.0
            pin.layoutY = index*32.0
            pin.updateUI()
        }

        outputPins.forEachIndexed { index, pin ->
            pin.layoutX = 256.0 + 96.0 - 128.0
            pin.layoutY = index*32.0
            pin.updateUI()
        }


        prefWidth = 256.0 + 96.0 + 1.0
        prefHeight = max(inputPins.size, outputPins.size)*32.0 + 1.0
        width = 256.0 + 96.0 + 1.0
        height = max(inputPins.size, outputPins.size)*32.0 + 1.0
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
}