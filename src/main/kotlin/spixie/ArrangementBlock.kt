package spixie

import javafx.application.Platform
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.layout.Region
import org.apache.commons.math3.fraction.Fraction
import spixie.components.ArrangementBlockInput
import spixie.components.Circle
import spixie.components.ParticleSpray

class ArrangementBlock(val zoom:FractionImmutablePointer): Region(), SpixieHashable {
    val visualEditor = VisualEditor()

    var strictWidth:Double
        get() = width
        set(value) {
            minWidth = value
            maxWidth = value
        }

    var strictHeight:Double
        get() = height
        set(value) {
            minHeight = value
            maxHeight = value
        }

    var timeStart = Fraction(0)
        set(value) {
            field = value
            updateZoom()
        }
    var timeEnd = Fraction(4)
        set(value) {
            field = value
            updateZoom()
        }

    var line = 0
        set(value) {
            field = value
            layoutY = value*100.0
        }

    val arrangementBlockInput = ArrangementBlockInput(50.0, 50.0)

    enum class Drag { BEGIN, END, NONE }

    init {
        updateZoom()
        strictHeight = 100.0

        setOnMouseMoved { event ->
            val mousePos = sceneToLocal(event.sceneX, event.sceneY)
            if(mousePos.x>strictWidth-10 || mousePos.x < 10){
                scene.cursor = Cursor.E_RESIZE
            }else{
                scene.cursor = Cursor.DEFAULT
            }
        }

        var mouseXOnStartDrag = 0.0
        var timeStartOnStartDrag = Fraction.ZERO
        var timeEndOnStartDrag = Fraction.ZERO
        var dragByThe = Drag.NONE

        setOnMousePressed { event ->
            if(event.button == MouseButton.PRIMARY){
                val mouseCoords = sceneToLocal(event.sceneX, event.sceneY)
                if(mouseCoords.x>strictWidth-10 && mouseCoords.x<10){
                    if(mouseCoords.x < strictWidth-mouseCoords.x){
                        mouseXOnStartDrag = event.screenX
                        timeStartOnStartDrag = timeStart
                        timeEndOnStartDrag = timeEnd
                        dragByThe = Drag.BEGIN
                        event.consume()
                    }else{
                        mouseXOnStartDrag = event.screenX
                        timeStartOnStartDrag = timeStart
                        timeEndOnStartDrag = timeEnd
                        dragByThe = Drag.END
                        event.consume()
                    }
                }else{
                    if(mouseCoords.x>strictWidth-10){
                        mouseXOnStartDrag = event.screenX
                        timeStartOnStartDrag = timeStart
                        timeEndOnStartDrag = timeEnd
                        dragByThe = Drag.END
                        event.consume()
                    }
                    if(mouseCoords.x<10){
                        mouseXOnStartDrag = event.screenX
                        timeStartOnStartDrag = timeStart
                        timeEndOnStartDrag = timeEnd
                        dragByThe = Drag.BEGIN
                        event.consume()
                    }
                }
            }
        }

        setOnMouseDragged { event ->
            if(event.isPrimaryButtonDown){
                var deltaX = Math.round((event.screenX - mouseXOnStartDrag)/25.0).toInt()
                if(dragByThe == Drag.BEGIN){
                    val newTimeStart = timeStartOnStartDrag.add(Fraction(deltaX*16).divide(zoom.value))
                    if(timeEnd.subtract(newTimeStart).compareTo(Fraction(16).divide(zoom.value)) >=0 ){
                        timeStart = newTimeStart
                        updateZoom()
                    }
                }
                if(dragByThe == Drag.END){
                    val newTimeEnd = timeEndOnStartDrag.add(Fraction(deltaX*16).divide(zoom.value))
                    if(newTimeEnd.subtract(timeStart).compareTo(Fraction(16).divide(zoom.value)) >=0 ){
                        timeEnd = newTimeEnd
                        updateZoom()
                    }
                }
            }
        }

        setOnMouseReleased { event ->
            if(event.button == MouseButton.PRIMARY){
                dragByThe = Drag.NONE
            }
            if(event.button == MouseButton.SECONDARY){
                Platform.runLater {
                    Main.workingWindow.nextOpen(visualEditor)
                }
            }
        }

        setOnMouseExited { scene.cursor = Cursor.DEFAULT }


        arrangementBlockInput.onValueInputOutputConnected = { a, b ->
            if(a is Node && b is Node){
                visualEditor.connectInputOutput(a,b)
            }
        }
        visualEditor.components.children.addAll(arrangementBlockInput)

        Main.world.time.onTimeChanged { time ->
            arrangementBlockInput.time.value.value = Math.max(0.0, Math.min(time - timeStart.toDouble(), timeEnd.subtract(timeStart).toDouble()))
        }
    }

    fun updateZoom(){
        strictWidth = Fraction(100, 64).multiply(timeEnd.subtract(timeStart)).multiply(zoom.value).toDouble()
        layoutX = Fraction(100, 64).multiply(timeStart).multiply(zoom.value).toDouble()
    }

    fun inTimeRange() = timeStart.toDouble() <= Main.world.time.time && timeEnd.toDouble() > Main.world.time.time

    fun render(renderBufferBuilder: RenderBufferBuilder){
        if(inTimeRange()){
            for (component in visualEditor.components.children) {
                if (component is ParticleSpray) {
                    component.autoStepParticles(Math.round(arrangementBlockInput.time.value.value*100).toInt())
                }
            }

            for (component in visualEditor.components.children) {
                if(component is ParticleSpray){
                    for (particle in component.particles) {
                        renderBufferBuilder.addParticle(particle.x, particle.y, particle.size, particle.red, particle.green, particle.blue, particle.alpha)
                    }
                }
                if(component is Circle){
                    component.render(renderBufferBuilder)
                }
            }
        }
    }

    override fun spixieHash(): Long {
        return visualEditor.spixieHash() mix timeStart.toDouble().raw() mix timeEnd.toDouble().raw() mix arrangementBlockInput.time.value.value.raw()
    }
}