package spixie.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import spixie.visualEditor.components.Graph
import spixie.visualEditor.pins.ComponentPin

class GraphSerializer : Serializer<Graph>() {
    override fun write(kryo: Kryo, output: Output, graph: Graph) {
        graph.serialize(kryo, output)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Graph>): Graph {
        val graph = Graph()
        graph.deserialize(kryo, input)
        return graph
    }
}