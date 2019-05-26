package spixie.visualEditor

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.CubicCurve
import javafx.scene.shape.Rectangle
import javafx.scene.shape.StrokeLineCap
import spixie.Core
import spixie.static.MAGIC
import spixie.static.initCustomPanning
import spixie.static.mix
import spixie.static.raw
import spixie.visualEditor.components.*
import spixie.visualEditor.pins.ComponentPin
import spixie.visualEditor.pins.ComponentPinFunc
import spixie.visualEditor.pins.ComponentPinNumber
import java.io.Serializable
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class Module(var name: String) {
    val contentPane = Pane()

    private val components = Group()

    val content = Group()

    private val connects = Group()

    private var selectedComponents = arrayOf<Component>()
        set(value) {
            selectedComponents.forEach { it.selected = false }
            field=value
            selectedComponents.forEach { it.selected = true }
        }

    val isMain
        get() = name == "Main"

    init {
        contentPane.initCustomPanning(content, true)
        content.children.addAll(components, connects)
        contentPane.apply { children.addAll(content) }


        var selectionRectangleStartPoint = Point2D(0.0, 0.0)
        contentPane.setOnMousePressed { event ->
            if(event.button == MouseButton.PRIMARY){
                selectionRectangleStartPoint = components.screenToLocal(event.screenX, event.screenY)

                val componentsUnderCursor = components.children
                        .map { it as Component }
                        .filter { it.boundsInParent.intersects(selectionRectangleStartPoint.x, selectionRectangleStartPoint.y, 0.0, 0.0) }.toTypedArray()

                if(componentsUnderCursor.isEmpty() || !componentsUnderCursor.all { selectedComponents.contains(it) }){
                    selectedComponents = componentsUnderCursor
                }
            }
        }

        contentPane.setOnMouseDragged { event ->
            if(event.button == MouseButton.PRIMARY){
                val selectionRectangleEndPoint = components.screenToLocal(event.screenX, event.screenY)
                selectedComponents = components.children
                        .map { it as Component }
                        .filter {
                            it.boundsInParent.intersects(
                                    min(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x),
                                    min(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y),
                                    max(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x) - min(selectionRectangleStartPoint.x, selectionRectangleEndPoint.x),
                                    max(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y) - min(selectionRectangleStartPoint.y, selectionRectangleEndPoint.y)
                            )
                        }
                        .toTypedArray()
            }
        }

        contentPane.setOnMouseClicked { event ->
            if(event.button == MouseButton.SECONDARY){
                val point2D = components.screenToLocal(event.screenX, event.screenY)
                openComponentsList(point2D) { result->
                    result.magneticRelocate(point2D.x - result.width / 2, point2D.y)
                    addComponent(result)
                }
            }
        }

        contentPane.clip = Rectangle(Double.MAX_VALUE, Double.MAX_VALUE)

        content.layoutXProperty().addListener { _, _, _ ->
            updateBackgroundGrid()
        }
        content.layoutYProperty().addListener { _, _, _ ->
            updateBackgroundGrid()
        }
    }

    fun openComponentsList(point2D: Point2D, result: (component: Component) -> Unit) {
        ComponentsList(point2D.x, point2D.y, content.children, isMain) {
            result(it)
        }
    }

    private fun clearComponents(){
        components.children.clear()
    }

    fun addComponent(component: Component){
        components.children.add(component)
        component.conneectionsChanged
                .startWith(Unit)
                .debounce(17L, TimeUnit.MILLISECONDS)
                .observeOn(JavaFxScheduler.platform())
                .subscribe{
                    reconnectPins()
                }
        component.disconnectPinRequest.subscribe { pinForDisconnect->
            components.children.forEach { component ->
                if(component is Component){
                    component.inputPins.forEach { pin ->
                        if(pin == pinForDisconnect){
                            pin.connections.clear()
                        }
                        if(pin.connections.contains(pinForDisconnect)){
                            pin.connections.remove(pinForDisconnect)
                        }
                    }
                }
            }
            component.conneectionsChanged.onNext(Unit)
        }
        component.relocateSelectedRequests.subscribe { (x, y)->
            selectedComponents.forEach {
                it.relativelyMagneticRelocate(x, y)
            }
        }
    }

    fun removeComponent(component: Component){
        components.children.remove(component)
    }

    fun findResultComponent(): ImageResult {
        val result = components.children.find { it is ImageResult } ?: throw Exception("Result component dont exist")
        return result as ImageResult
    }

    fun findParticlesResultComponent(): ParticlesResult {
        val result = components.children.find { it is ParticlesResult } ?: throw Exception("Result component dont exist")
        return result as ParticlesResult
    }

    fun findResultComponentNode(): Node {
        return components.children.find { it is ImageResult || it is ParticlesResult } ?: throw Exception("Result component dont exist")
    }

    fun updateModuleComponents(){
        components.children.forEach { component->
            if(component is ModuleComponent){
                component.module = Core.arrangementWindow.visualEditor.modules.find { it.name == component.externalName }
            }
        }
    }

    fun calcHashOfConsts(): Long {
        var result = MAGIC.toLong()
        val discovered = hashSetOf<Component>()
        val componentsList = arrayListOf<Component>(findResultComponent())
        while (componentsList.isNotEmpty()){
            val first = componentsList.removeAt(0)
            discovered.add(first)
            result = result mix first.hashCode().toLong()
            first.inputPins.forEach {
                val connections = it.connections
                if(connections.isEmpty()){
                    connections.forEach {
                        if(!discovered.contains(it.component)){
                            componentsList.add(it.component)
                        }
                    }
                }else{
                    if(it is ComponentPinNumber){
                        it.valueControl?.let { valueControl ->
                            result = result mix valueControl.value.raw()
                        }
                    }
                }
            }
        }
        return result
    }

    fun reconnectPins(){
        connects.children.clear()
        contentPane.layout()
        components.children.forEach { component ->
            if(component is Component){
                component.inputPins.forEach { pin1 ->
                    pin1.connections.forEach { pin2->
                        connectPins(pin2, pin1, Color.DARKVIOLET)
                    }
                }
            }
        }
        Core.renderManager.requestRender()
    }

    private fun connectPins(pin1: ComponentPin, pin2: ComponentPin, color: Color){
        val cubicCurve = CubicCurve()
        val aBounds = pin1.component.localToParent(pin1.component.content.localToParent(pin1.localToParent(pin1.circle.boundsInParent)))
        val bBounds = pin2.component.localToParent(pin2.component.content.localToParent(pin2.localToParent(pin2.circle.boundsInParent)))
        cubicCurve.startX = (aBounds.minX + aBounds.maxX) / 2
        cubicCurve.startY = (aBounds.minY + aBounds.maxY) / 2
        cubicCurve.endX = (bBounds.minX + bBounds.maxX) / 2
        cubicCurve.endY = (bBounds.minY + bBounds.maxY) / 2
        cubicCurve.controlX1 = cubicCurve.startX + (cubicCurve.endX - cubicCurve.startX).absoluteValue.coerceIn(42.0..128.0)/2
        cubicCurve.controlY1 = cubicCurve.startY
        cubicCurve.controlX2 = cubicCurve.endX - (cubicCurve.endX - cubicCurve.startX).absoluteValue.coerceIn(42.0..128.0)/2
        cubicCurve.controlY2 = cubicCurve.endY
        cubicCurve.fill = Color.TRANSPARENT
        cubicCurve.strokeWidth = 3.0
        cubicCurve.stroke = color
        connects.children.add(cubicCurve)

        cubicCurve.isMouseTransparent = true
        cubicCurve.strokeLineCap = StrokeLineCap.ROUND
    }

    fun serialize(): SerializedModule {
        val componentsList = components.children.filter { it is Component }.map { it as Component }
        componentsList.forEachIndexed { index, component -> component.serializationIndex = index }
        val inputToOutputConnection = arrayListOf<Pair<ComponentPin, ComponentPin>>()
        this.components.children.forEach { component ->
            if(component is Component){
                component.inputPins.forEach { pin1 ->
                    pin1.connections.forEach { pin2->
                        inputToOutputConnection.add(pin1 to pin2)
                    }
                }
            }
        }
        val connections = inputToOutputConnection.map { entry ->
            val v1 = entry.first.component.serializationIndex to entry.first.name
            val v2 = entry.second.component.serializationIndex to entry.second.name
            v1 to v2
        }
        val gson = Gson()
        val inputPins = componentsList.map { component -> component.inputPins.map { gson.toJson(it.serialize()) } }
        return SerializedModule(name, componentsList, connections, inputPins)
    }

    fun deserizalize(data: SerializedModule) {
        clearComponents()
        data.components.forEach { addComponent(it) }
        val idToComponent = data.components.map { it.serializationIndex to it }.toMap()
        val gson = Gson()
        components.children.forEachIndexed { index, component ->
            if(component is Component){
                component.inputPins.addAll(data.inputPins[index].map { ComponentPin.deserialize(gson.fromJson(it, JsonObject::class.java)) })
                component.configInit()
            }
        }

        components.children.forEachIndexed { index, component ->
            if(component is Component){
                component.inputPins.forEach { pin1 ->
                    pin1.connections.addAll(
                            data.connections.mapNotNull {
                                if(pin1.component.serializationIndex == it.first.first && pin1.name == it.first.second){
                                    it.second
                                }else{
                                    null
                                }
                            }.mapNotNull { pin2Serialized->
                                idToComponent[pin2Serialized.first]?.outputPins?.find { it.name == pin2Serialized.second }
                            }
                    )
                }
            }
        }
    }

    private fun updateBackgroundGrid(){
        contentPane.style = "-fx-background-color: #FFFFFFFF, linear-gradient(from ${content.layoutX+0.5}px 0px to ${content.layoutX+(VE_GRID_CELL_SIZE/2.0+0.5)}px 0px, repeat, #00000022 5%, transparent 5%),linear-gradient(from 0px ${content.layoutY+0.5}px to 0px ${content.layoutY+(VE_GRID_CELL_SIZE/2.0+0.5)}px, repeat, #00000022 5%, transparent 5%);"
    }

    override fun toString(): String {
        return name
    }

    class SerializedModule(val name: String, val components: List<Component>, val connections: List<Pair<Pair<Int, String>, Pair<Int, String>>>, val inputPins: List<List<String>>) : Serializable
}