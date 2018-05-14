package spixie.visualEditor

import javafx.scene.Group
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.CubicCurve
import spixie.Main
import spixie.static.MAGIC
import spixie.static.initCustomPanning
import spixie.static.mix
import spixie.static.raw
import spixie.visualEditor.components.ModuleComponent
import spixie.visualEditor.components.Result
import kotlin.math.absoluteValue

class Module(var name: String) {
    val contentPane = Pane()
    private val components = Group()
    val content = Group()
    private val connects = Group()

    val isMain
        get() = name == "Main"

    init {
        contentPane.initCustomPanning(content, true)
        content.children.addAll(components, connects)
        addComponent(Result())
        contentPane.apply { children.addAll(content) }

        contentPane.setOnMouseClicked { event ->
            if(event.button == MouseButton.SECONDARY){
                val screenToLocal = components.screenToLocal(event.screenX, event.screenY)
                ComponentsList(screenToLocal.x, screenToLocal.y, content.children, isMain) { result ->
                    result.magneticRelocate(screenToLocal.x - result.width / 2, screenToLocal.y)
                    addComponent(result)
                }
            }
        }

        content.layoutXProperty().addListener { _, _, _ ->
            updateBackgroundGrid()
        }
        content.layoutYProperty().addListener { _, _, _ ->
            updateBackgroundGrid()
        }
    }

    fun clearComponents(){
        components.children.clear()
    }

    fun addComponent(component: Component){
        component.relocations.subscribe {
            reconnectPins()
        }
        component.conneectionsChanged.subscribe{
            reconnectPins()
        }
        component.disconnectPinRequest.subscribe { pinForDisconnect->
            components.children.forEach { component ->
                if(component is Component){
                    component.inputPins.forEach { pin ->
                        if(pin.connection == pinForDisconnect || pin == pinForDisconnect){
                            pin.connection = null
                        }
                    }
                }
            }
        }
        components.children.add(component)
    }

    fun findResultComponent(): Result {
        val result = components.children.find { it is Result } ?: throw Exception("Result component dont exist")
        return result as Result
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
                val connection = it.connection
                if(connection != null){
                    if(!discovered.contains(connection.component)){
                        componentsList.add(connection.component)
                    }
                }else{
                    it.valueControl?.let { valueControl ->
                        result = result mix valueControl.value.raw()
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
                    pin1.connection?.let { pin2->
                        connectPins(pin2, pin1)
                    }
                }
            }
        }
        Main.renderManager.requestRender()
    }


    private fun connectPins(pin1: ComponentPin<*>, pin2: ComponentPin<*>){
        val cubicCurve = CubicCurve()
        val aBounds = pin1.component.localToParent(pin1.component.content.localToParent(pin1.localToParent(pin1.circle.boundsInParent)))
        val bBounds = pin2.component.localToParent(pin2.component.content.localToParent(pin2.localToParent(pin2.circle.boundsInParent)))
        cubicCurve.startX = (aBounds.minX + aBounds.maxX) / 2
        cubicCurve.startY = (aBounds.minY + aBounds.maxY) / 2
        cubicCurve.endX = (bBounds.minX + bBounds.maxX) / 2
        cubicCurve.endY = (bBounds.minY + bBounds.maxY) / 2
        cubicCurve.controlX1 = cubicCurve.startX + (cubicCurve.endX - cubicCurve.startX).absoluteValue.coerceIn(64.0..800.0)/2
        cubicCurve.controlY1 = cubicCurve.startY
        cubicCurve.controlX2 = cubicCurve.endX - (cubicCurve.endX - cubicCurve.startX).absoluteValue.coerceIn(64.0..800.0)/2
        cubicCurve.controlY2 = cubicCurve.endY
        cubicCurve.fill = Color.TRANSPARENT
        cubicCurve.strokeWidth = 4.0
        cubicCurve.stroke = Color.DARKVIOLET
        connects.children.add(cubicCurve)

        cubicCurve.isMouseTransparent = true
    }

    fun toSerializable(): Triple<String, List<Component>, List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> {
        val components = components.children.filter { it is Component }.map { it as Component }
        components.forEachIndexed { index, component -> component.serializationIndex = index }
        val inputToOutputConnection = hashMapOf<ComponentPin<*>, ComponentPin<*>>()
        this.components.children.forEach { component ->
            if(component is Component){
                component.inputPins.forEach { pin1 ->
                    pin1.connection?.let { pin2->
                        inputToOutputConnection[pin1] = pin2
                    }
                }
            }
        }
        val connections = inputToOutputConnection.map { entry ->
            val v1 = entry.key.component.serializationIndex to entry.key.component.inputPins.indexOf(entry.key)
            val v2 = entry.value.component.serializationIndex to entry.value.component.outputPins.indexOf(entry.value)
            v1 to v2
        }
        return Triple(name, components, connections)
    }

    fun fromSerializable(data: Triple<String, List<Component>, List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>) {
        val (moduleName, moduleComponents, moduleConnections) = data
        clearComponents()
        moduleComponents.forEach { addComponent(it) }
        val idToComponent = moduleComponents.map { it.serializationIndex to it }.toMap()

        val inputToOutputConnection = hashMapOf<ComponentPin<*>, ComponentPin<*>>()
        moduleConnections.forEach {
            val (a,b) = it
            val (componentInputIndex, inputIndex) = a
            val (componentOutputIndex, outputIndex) = b
            idToComponent[componentInputIndex]?.inputPins?.get(inputIndex)?.let { inputPin ->
                idToComponent[componentOutputIndex]?.outputPins?.get(outputIndex)?.let { outputPin ->
                    inputToOutputConnection[inputPin] = outputPin
                }
            }
        }
        components.children.forEach { component ->
            if(component is Component){
                component.inputPins.forEach { pin1 ->
                    pin1.connection = inputToOutputConnection[pin1]
                }
            }
        }
    }

    private fun updateBackgroundGrid(){
        contentPane.style = "-fx-background-color: #FFFFFFFF, linear-gradient(from ${content.layoutX+0.5}px 0px to ${content.layoutX+16.5}px 0px, repeat, #00000022 5%, transparent 5%),linear-gradient(from 0px ${content.layoutY+0.5}px to 0px ${content.layoutY+16.5}px, repeat, #00000022 5%, transparent 5%);"
    }

    override fun toString(): String {
        return name
    }
}