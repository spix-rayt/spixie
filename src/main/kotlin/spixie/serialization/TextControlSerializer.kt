package spixie.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import spixie.TextControl

class TextControlSerializer : Serializer<TextControl>() {
    override fun write(kryo: Kryo, output: Output, textControl: TextControl) {
        output.writeString(textControl.value)
        output.writeString(textControl.name)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out TextControl>): TextControl {
        val value = input.readString()
        val name = input.readString()
        return TextControl(value, name)
    }
}