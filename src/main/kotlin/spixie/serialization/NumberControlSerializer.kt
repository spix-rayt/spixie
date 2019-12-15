package spixie.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import spixie.NumberControl

class NumberControlSerializer : Serializer<NumberControl>() {
    override fun write(kryo: Kryo, output: Output, numberControl: NumberControl) {
        output.writeDouble(numberControl.value)
        output.writeString(numberControl.name)
        output.writeDouble(numberControl.numberLineScale)
        output.writeDouble(numberControl.min)
        output.writeDouble(numberControl.max)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out NumberControl>): NumberControl {
        val value = input.readDouble()
        val name = input.readString()
        val numberLineScale = input.readDouble()
        val min = input.readDouble()
        val max = input.readDouble()

        return NumberControl(value, name, numberLineScale).limitMin(min).limitMax(max)
    }
}