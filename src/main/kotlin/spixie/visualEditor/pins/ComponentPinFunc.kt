package spixie.visualEditor.pins

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import spixie.NoArg
import spixie.visualEditor.Component
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.reflect.KClass

class ComponentPinFunc(name: String): ComponentPin(name) {
    var getValue: ((t: Double) -> Double)? = null

    fun receiveValue(t: Double): Double {
        if(connections.isEmpty()){
            return 1.0
        }else{
            return connections
                    .mapNotNull { (it as? ComponentPinFunc)?.getValue?.invoke(t) }.sum()
        }
    }
}