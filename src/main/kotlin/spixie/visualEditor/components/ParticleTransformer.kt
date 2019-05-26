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
    private val inParticles by lazyPinFromListOrCreate(0) { ComponentPinParticleArray("Particles") }

    private val outParticles = ComponentPinParticleArray("Particles").apply {
        getValue = {
            val particles = inParticles.receiveValue()
            inputPins.filter { it != inParticles }.forEach { pin ->
                if (pin is ComponentPinNumber) {
                    if (pin.name == TransformerType.Hue.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.hue = v }
                    }
                    if (pin.name == TransformerType.Chroma.name) {
                        val v = pin.receiveValue().toFloat().coerceIn(0.0f, 1.0f)
                        particles.array.forEach { it.chroma = v }
                    }
                    if (pin.name == TransformerType.Luminance.name) {
                        val v = pin.receiveValue().toFloat().coerceAtLeast(0.0f)
                        particles.array.forEach { it.luminance = v }
                    }
                    if (pin.name == TransformerType.Transparency.name) {
                        val v = pin.receiveValue().toFloat().coerceIn(0.0f, 1.0f)
                        particles.array.forEach { it.transparency = v }
                    }
                    if (pin.name == TransformerType.Scale.name) {
                        val v = pin.receiveValue().toFloat().coerceAtLeast(0.0f)
                        particles.array.forEach { it.matrix.scaleLocal(v) }
                    }
                    if (pin.name == TransformerType.Size.name) {
                        val v = pin.receiveValue().toFloat().coerceAtLeast(0.0f)
                        particles.array.forEach { it.size = v }
                    }
                    if (pin.name == TransformerType.Edge.name) {
                        val v = pin.receiveValue().toFloat().coerceIn(0.0f, 1.0f)
                        particles.array.forEach { it.edge = v }
                    }

                    if (pin.name == TransformerType.PositionX.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.translateLocal(v, 0.0f, 0.0f) }
                    }
                    if (pin.name == TransformerType.PositionY.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.translateLocal(0.0f, v, 0.0f) }
                    }
                    if (pin.name == TransformerType.PositionZ.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.translateLocal(0.0f, 0.0f, v) }
                    }

                    if (pin.name == TransformerType.RotateX.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.rotateLocalX(v) }
                    }
                    if (pin.name == TransformerType.RotateY.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.rotateLocalY(v) }
                    }
                    if (pin.name == TransformerType.RotateZ.name) {
                        val v = pin.receiveValue().toFloat()
                        particles.array.forEach { it.matrix.rotateLocalZ(v) }
                    }
                }
                if (pin is ComponentPinFunc) {
                    if (pin.name == TransformerType.Hue.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.hue = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.name == TransformerType.Chroma.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.chroma = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.name == TransformerType.Luminance.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.luminance = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.name == TransformerType.Transparency.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.transparency = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.name == TransformerType.Scale.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.scaleLocal(pin.receiveValue(t).toFloat())
                        }
                    }
                    if (pin.name == TransformerType.Size.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.size = pin.receiveValue(t).toFloat()
                        }
                    }
                    if (pin.name == TransformerType.Edge.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.edge = pin.receiveValue(t).toFloat()
                        }
                    }

                    if (pin.name == TransformerType.PositionX.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.translateLocal(pin.receiveValue(t).toFloat(), 0.0f, 0.0f)
                        }
                    }
                    if (pin.name == TransformerType.PositionY.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.translateLocal(0.0f, pin.receiveValue(t).toFloat(), 0.0f)
                        }
                    }
                    if (pin.name == TransformerType.PositionZ.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.translateLocal(0.0f, 0.0f, pin.receiveValue(t).toFloat())
                        }
                    }

                    if (pin.name == TransformerType.RotateX.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.rotateLocalX((pin.receiveValue(t) * Math.PI * 2.0).toFloat())
                        }
                    }
                    if (pin.name == TransformerType.RotateY.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.rotateLocalY((pin.receiveValue(t) * Math.PI * 2.0).toFloat())
                        }
                    }
                    if (pin.name == TransformerType.RotateZ.name) {
                        particles.forEachWithGradient { t, particle ->
                            particle.matrix.rotateLocalZ((pin.receiveValue(t) * Math.PI * 2.0).toFloat())
                        }
                    }
                }
            }
            particles
        }
    }

    override fun creationInit() {
        inputPins.addAll(arrayListOf(inParticles))
    }

    override fun configInit() {
        inParticles.contextMenu.items.addAll(createMenuItemsForPin(inParticles))
        outputPins.add(outParticles)
        updateUI()
    }

    private fun createMenuItemsForPin(componentPin: ComponentPin): ArrayList<MenuItem> {
        val result = arrayListOf<MenuItem>()

        if(componentPin is ComponentPinNumber) {
            result.add(
                    MenuItem("Switch to Func").apply {
                        setOnAction {
                            componentPin.unconnectAll()
                            val pin = ComponentPinFunc(componentPin.name)
                            pin.contextMenu.items.addAll(createMenuItemsForPin(pin))
                            val index = inputPins.indexOf(componentPin)
                            inputPins.set(index, pin)
                            updateUI()
                        }
                    }
            )
        }
        if(componentPin is ComponentPinFunc) {
            result.add(
                    MenuItem("Switch to Num").apply {
                        setOnAction {
                            componentPin.unconnectAll()
                            val pin = ComponentPinNumber(componentPin.name, NumberControl(0.0, ""))
                            pin.contextMenu.items.addAll(createMenuItemsForPin(pin))
                            val index = inputPins.indexOf(componentPin)
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
                            val pin = ComponentPinNumber(transformerType.name, NumberControl(0.0, ""))
                            pin.contextMenu.items.addAll(createMenuItemsForPin(pin))
                            val index = inputPins.indexOf(componentPin)
                            inputPins.add(index + 1, pin)
                            updateUI()
                        }
                    }
            )
        }
        return result
    }

    companion object {
        const val serialVersionUID = 0L
    }
}