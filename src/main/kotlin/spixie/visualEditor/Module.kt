package spixie.visualEditor

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
import spixie.Main
import spixie.static.MAGIC
import spixie.static.initCustomPanning
import spixie.static.mix
import spixie.static.raw
import spixie.visualEditor.components.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
                component.module = Main.arrangementWindow.visualEditor.modules.find { it.name == component.externalName }
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
        components.children.forEach { component->
            if(component is WithParticlesArrayInput){
                component.getParticlesArrayInput().imaginaryConnections.clear()
            }
        }
        components.children.forEach { component ->
            if(component is Component){
                component.inputPins.forEach { pin1 ->
                    pin1.connections.forEach { pin2->
                        connectPins(pin2, pin1, Color.DARKVIOLET)
                    }
                }
                if(component is WithParticlesArrayOutput){
                    findComponentBelowOf(component).let { belowComponent ->
                        if(belowComponent is WithParticlesArrayInput) {
                            val outputPin = component.getParticlesArrayOutput()
                            val inputPin = belowComponent.getParticlesArrayInput()
                            connectPins(outputPin, inputPin, Color.DARKVIOLET.deriveColor(0.0, 1.0, 1.0, 0.15))
                            inputPin.imaginaryConnections.add(outputPin)
                        }
                    }
                }
            }
        }
        Main.renderManager.requestRender()
    }

    fun findComponentBelowOf(component: Component): Component? {
        components.children.forEach { component2 ->
            if(component2 is Component){
                if(component.layoutX.roundToInt() == component2.layoutX.roundToInt()){
                    if((component.layoutY+(component.height-1)).roundToInt() == component2.layoutY.roundToInt()){
                        return component2
                    }
                }
            }
        }
        return null
    }

    fun findComponentAboveOf(component: Component): Component? {
        components.children.forEach { component2 ->
            if(component2 is Component){
                if(component.layoutX.roundToInt() == component2.layoutX.roundToInt()){
                    if(component.layoutY.roundToInt() == (component2.layoutY + (component2.height-1)).roundToInt()){
                        return component2
                    }
                }
            }
        }
        return null
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

    fun toSerializable(): Triple<String, List<Component>, List<Pair<Pair<Int, String>, Pair<Int, String>>>> {
        val components = components.children.filter { it is Component }.map { it as Component }
        components.forEachIndexed { index, component -> component.serializationIndex = index }
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
        return Triple(name, components, connections)
    }

    fun fromSerializable(data: Triple<String, List<Component>, List<Pair<Pair<Int, String>, Pair<Int, String>>>>) {
        val (moduleName, moduleComponents, moduleConnections) = data
        clearComponents()
        moduleComponents.forEach { addComponent(it) }
        val idToComponent = moduleComponents.map { it.serializationIndex to it }.toMap()
        components.children.forEach { component ->
            if(component is Component){
                component.inputPins.forEach { pin1 ->
                    pin1.connections.addAll(
                            moduleConnections.mapNotNull {
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
}