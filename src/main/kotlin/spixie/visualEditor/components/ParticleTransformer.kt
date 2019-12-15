package spixie.visualEditor.components

import javafx.scene.control.MenuItem
import spixie.NumberControl
import spixie.visualEditor.*
import spixie.visualEditor.pins.ComponentPin
import spixie.visualEditor.pins.ComponentPinFunc
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray

enum class TransformerType {
    Hue, Chroma, Luminance, Transparency, PositionX, PositionY, PositionZ, RotateX, RotateY, RotateZ, Scale, Size, Edge
}

class ParticleTransformer: Component() {
    private val inParticles = ComponentPinParticleArray("Particles")

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particles = inParticles.receiveValue()
            getTransformPinsList().forEach { pin ->
                if (pin is ComponentPinNumber) {
                    if (pin.typeString == TransformerType.Hue.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.hue = v }
                    }
                    if (pin.typeString == TransformerType.Chroma.name) {
                        val v = pin.receiveValue().toFloat().coerceIn(0.0f, 1.0f)
                        particles.array.forEach { it.chroma = v }
                    }
                    if (pin.typeString == TransformerType.Luminance.name) {
                        val v = pin.receiveValue().toFloat().coerceAtLeast(0.0f)
                        particles.array.forEach { it.luminance = v }
                    }
                    if (pin.typeString == TransformerType.Transparency.name) {
                        val v = pin.receiveValue().toFloat().coerceIn(0.0f, 1.0f)
                        particles.array.forEach { it.transparency = v }
                    }
                    if (pin.typeString == TransformerType.Scale.name) {
                        val v = pin.receiveValue().toFloat().coerceAtLeast(0.0f)
                        particles.array.forEach { it.matrix.scaleLocal(v) }
                    }
                    if (pin.typeString == TransformerType.Size.name) {
                        val v = pin.receiveValue().toFloat().coerceAtLeast(0.0f)
                        particles.array.forEach { it.size = v }
                    }
                    if (pin.typeString == TransformerType.Edge.name) {
                        val v = pin.receiveValue().toFloat().coerceIn(0.0f, 1.0f)
                        particles.array.forEach { it.edge = v }
                    }

                    if (pin.typeString == TransformerType.PositionX.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.translateLocal(v, 0.0f, 0.0f) }
                    }
                    if (pin.typeString == TransformerType.PositionY.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.translateLocal(0.0f, v, 0.0f) }
                    }
                    if (pin.typeString == TransformerType.PositionZ.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.translateLocal(0.0f, 0.0f, v) }
                    }

                    if (pin.typeString == TransformerType.RotateX.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.rotateLocalX(v) }
                    }
                    if (pin.typeString == TransformerType.RotateY.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.rotateLocalY(v) }
                    }
                    if (pin.typeString == TransformerType.RotateZ.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.rotateLocalZ(v) }
                    }
                }
                if (pin is ComponentPinFunc) {
                    if (pin.typeString == TransformerType.Hue.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.hue = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.typeString == TransformerType.Chroma.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.chroma = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.typeString == TransformerType.Luminance.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.luminance = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.typeString == TransformerType.Transparency.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.transparency = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.typeString == TransformerType.Scale.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.scaleLocal(pin.receiveValue(t).toFloat())
                        }
                    }
                    if (pin.typeString == TransformerType.Size.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.size = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.typeString == TransformerType.Edge.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.edge = pin.receiveValue(t).toFloat()
                        }
                    }

                    if (pin.typeString == TransformerType.PositionX.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.translateLocal(pin.receiveValue(t).toFloat(), 0.0f, 0.0f)
                        }
                    }
                    if (pin.typeString == TransformerType.PositionY.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.translateLocal(0.0f, pin.receiveValue(t).toFloat(), 0.0f)
                        }
                    }
                    if (pin.typeString == TransformerType.PositionZ.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.translateLocal(0.0f, 0.0f, pin.receiveValue(t).toFloat())
                        }
                    }

                    if (pin.typeString == TransformerType.RotateX.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.rotateLocalX((pin.receiveValue(t) * Math.PI * 2.0).toFloat())
                        }
                    }
                    if (pin.typeString == TransformerType.RotateY.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.rotateLocalY((pin.receiveValue(t) * Math.PI * 2.0).toFloat())
                        }
                    }
                    if (pin.typeString == TransformerType.RotateZ.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.rotateLocalZ((pin.receiveValue(t) * Math.PI * 2.0).toFloat())
                        }
                    }
                }
            }
            particles
        }
    }

    init {
        inputPins.addAll(arrayListOf(inParticles))
        inParticles.contextMenu.items.addAll(createMenuItemsForPin(inParticles))
        outputPins.add(outParticles)
        updateUI()
    }

    fun getTransformPinsList(): List<ComponentPin> {
        return inputPins.filter { it != inParticles }
    }

    private fun createMenuItemsForPin(clickedComponentPin: ComponentPin): ArrayList<MenuItem> {
        val result = arrayListOf<MenuItem>()

        if(clickedComponentPin is ComponentPinNumber) {
            result.add(
                    MenuItem("Switch to Func").apply {
                        setOnAction {
                            clickedComponentPin.unconnectAll()
                            val pin = ComponentPinFunc(clickedComponentPin.name)
                            pin.typeString = clickedComponentPin.typeString
                            pin.contextMenu.items.addAll(createMenuItemsForPin(pin))
                            val index = inputPins.indexOf(clickedComponentPin)
                            inputPins.set(index, pin)
                            updateUI()
                        }
                    }
            )
        }
        if(clickedComponentPin is ComponentPinFunc) {
            result.add(
                    MenuItem("Switch to Num").apply {
                        setOnAction {
                            clickedComponentPin.unconnectAll()
                            val pin = ComponentPinNumber(clickedComponentPin.name, NumberControl(0.0, ""))
                            pin.typeString = clickedComponentPin.typeString
                            pin.contextMenu.items.addAll(createMenuItemsForPin(pin))
                            val index = inputPins.indexOf(clickedComponentPin)
                            inputPins.set(index, pin)
                            updateUI()
                        }
                    }
            )
        }

        TransformerType.values().forEach { transformerType ->
            result.add(
                    MenuItem("Add $transformerType pin").apply {
                        setOnAction {
                            var n = -1
                            var name = "${transformerType.name}~$n"
                            while (inputPins.find { it.name == name } != null) {
                                n++
                                name = "${transformerType.name}~$n"
                            }
                            val pin = ComponentPinNumber(name, NumberControl(0.0, ""))
                            pin.typeString = transformerType.name
                            pin.contextMenu.items.addAll(createMenuItemsForPin(pin))
                            val index = inputPins.indexOf(clickedComponentPin)
                            inputPins.add(index + 1, pin)
                            updateUI()
                        }
                    }
            )
        }
        return result
    }
}