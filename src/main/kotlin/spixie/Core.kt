package spixie

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import spixie.arrangement.ArrangementGraph
import spixie.arrangement.ArrangementGraphsContainer
import spixie.arrangement.ArrangementWindow
import spixie.arrangement.SerializedProject
import spixie.opencl.OpenCLApi
import spixie.serialization.*
import spixie.visualEditor.GraphData
import spixie.visualEditor.Module
import spixie.visualEditor.components.*
import spixie.visualEditor.pins.ComponentPinFunc
import spixie.visualEditor.pins.ComponentPinImageFloatBuffer
import spixie.visualEditor.pins.ComponentPinNumber
import spixie.visualEditor.pins.ComponentPinParticleArray
import java.util.*
import kotlin.collections.ArrayList

object Core {
    val renderManager by lazy { RenderManager() }
    val workingWindow by lazy { WorkingWindow() }
    val arrangementWindow by lazy { ArrangementWindow() }
    val audio by lazy { Audio() }
    val opencl by lazy { OpenCLApi() }

    val kryo = Kryo().apply {
        references = true
        register(Module.SerializedModule::class.java, 100)
        register(SerializedProject::class.java, 101)
        register(ArrangementGraphsContainer::class.java, 103)
        register(ArrangementGraph::class.java, 104)
        register(GraphData::class.java, 105)
        register(GraphData.Fragment::class.java, 106)
        register(ArrayList::class.java, 107)
        register(List::class.java, DefaultSerializers.ArraysAsListSerializer(), 108)
        register(FloatArray::class.java, 109)
        register(Collections.singletonList("")::class.java, DefaultSerializers.ArraysAsListSerializer(), 110)
        register(Collections.EMPTY_LIST::class.java, DefaultSerializers.ArraysAsListSerializer(), 111)
        register(emptyList<Int>()::class.java, DefaultSerializers.ArraysAsListSerializer(), 112)
        register(Module.PinAddress::class.java, 113)
        register(Module.PinConnection::class.java, 114)
        register(Module.NumberPinInternalValue::class.java, 115)

        register(Color::class.java, ComponentSerializer(), 10000)
        register(Func::class.java, ComponentSerializer(), 10001)
        register(FuncLinear::class.java, ComponentSerializer(), 10002)
        register(FuncRandom::class.java, ComponentSerializer(), 10003)
        register(FuncSin::class.java, ComponentSerializer(), 10004)
        register(Graph::class.java, GraphSerializer(), 10005)
        register(ImageResult::class.java, ComponentSerializer(), 10006)
        register(LineTest::class.java, ComponentSerializer(), 10007)
        register(ModFilter::class.java, ComponentSerializer(), 10008)
        register(MoveRotate::class.java, ComponentSerializer(), 10009)
        register(ParticlesProduct::class.java, ComponentSerializer(), 10010)
        register(ParticlesResult::class.java, ComponentSerializer(), 10011)
        register(ParticleTransformer::class.java, ComponentSerializer(), 10012)
        register(Render::class.java, ComponentSerializer(), 10013)
        register(SimpleParticlesGenerator::class.java, ComponentSerializer(), 10014)
        register(Slice::class.java, ComponentSerializer(), 10015)

        register(ComponentPinFunc::class.java, ComponentPinSerializer(), 11000)
        register(ComponentPinImageFloatBuffer::class.java, ComponentPinSerializer(), 11001)
        register(ComponentPinNumber::class.java, ComponentPinSerializer(), 11002)
        register(ComponentPinParticleArray::class.java, ComponentPinSerializer(), 11003)

        register(NumberControl::class.java, NumberControlSerializer(), 12000)
        register(TextControl::class.java, TextControlSerializer(), 12001)
    }

    var dragAndDropObject: Any = Any()
}