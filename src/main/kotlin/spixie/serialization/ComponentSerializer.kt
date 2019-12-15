package spixie.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import spixie.visualEditor.Component
import spixie.visualEditor.components.ParticleTransformer
import spixie.visualEditor.pins.ComponentPin

class ComponentSerializer : Serializer<Component>() {
    override fun write(kryo: Kryo, output: Output, component: Component) {
        output.writeDouble(component.layoutX)
        output.writeDouble(component.layoutY)
        if(component is ParticleTransformer) {
            kryo.writeObject(output, component.getTransformPinsList())
        }
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Component>): Component {
        val newInstance = type.newInstance()
        val x = input.readDouble()
        val y = input.readDouble()
        newInstance.magneticRelocate(x, y)

        if(type == ParticleTransformer::class.java) {
            val transformPins = kryo.readObject(input, List::class.java) as List<ComponentPin>
            newInstance.inputPins.addAll(transformPins)
        }
        newInstance.updateUI()
        return newInstance
    }
}