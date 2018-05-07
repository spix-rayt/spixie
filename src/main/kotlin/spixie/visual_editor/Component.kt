package spixie.visual_editor

import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.input.MouseButton
import javafx.scene.layout.Region
import spixie.Main
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.*

open class Component:Region(), Externalizable {
    val inputPins = arrayListOf<ComponentPin<*>>()
    val outputPins = arrayListOf<ComponentPin<*>>()
    val content = Group()

    var dragDelta = Point2D(0.0, 0.0)

    init {
        style = "-fx-border-color: #9A12B3FF; -fx-border-width: 1; -fx-background-color: #FFFFFFFF;"

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

        children.addAll(content)
    }

    fun magneticRelocate(x: Double, y:Double){
        val newX = (x / 32.0).roundToInt() * 32.0
        val newY = floor(y / 32.0) * 32.0
        if(layoutX != newX || layoutY != newY){
            relocate(newX, newY)
            Main.arrangementWindow.visualEditor.reconnectPins()
        }
    }

    fun updateVisual(){
        content.children.clear()
        content.children.addAll(inputPins)
        content.children.addAll(outputPins)
        inputPins.forEachIndexed { index, pin ->
            pin.layoutX = 0.0
            pin.layoutY = index*32.0
            pin.relocateNodes()
        }

        outputPins.forEachIndexed { index, pin ->
            pin.layoutX = 256.0 + 96.0 - 128.0
            pin.layoutY = index*32.0
            pin.relocateNodes()
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
        o.writeInt(serializationIndex)
    }

    override fun readExternal(o: ObjectInput) {
        magneticRelocate(o.readDouble(), o.readDouble())
        serializationIndex = o.readInt()
    }
}