package spixie.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import spixie.NumberControl
import spixie.visualEditor.pins.*

class ComponentPinSerializer : Serializer<ComponentPin>() {
    override fun write(kryo: Kryo, output: Output, componentPin: ComponentPin) {
        output.writeString(componentPin.name)
        output.writeString(componentPin.typeString)
        if(componentPin is ComponentPinNumber) {
            kryo.writeObjectOrNull(output, componentPin.valueControl, NumberControl::class.java)
        }
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out ComponentPin>): ComponentPin {
        val name = input.readString()
        val typeString = input.readString()
        return when(type) {
            ComponentPinFunc::class.java -> {
                ComponentPinFunc(name).apply {
                    this.typeString = typeString
                }
            }
            ComponentPinImageFloatBuffer::class.java -> {
                ComponentPinImageFloatBuffer(name).apply {
                    this.typeString = typeString
                }
            }
            ComponentPinNumber::class.java -> {
                val numberControl = kryo.readObjectOrNull(input, NumberControl::class.java)
                ComponentPinNumber(name, numberControl).apply {
                    this.typeString = typeString
                }
            }
            ComponentPinParticleArray::class.java -> {
                ComponentPinParticleArray(name).apply {
                    this.typeString = typeString
                }
            }
            else -> throw ClassNotFoundException(type.name)
        }
    }
}