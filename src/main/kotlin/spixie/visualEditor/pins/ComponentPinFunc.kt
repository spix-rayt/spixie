package spixie.visualEditor.pins

class ComponentPinFunc(name: String): ComponentPin(name) {
    var getValue: ((t: Double) -> Double)? = null

    fun receiveValue(t: Double): Double {
        if(connections.isEmpty()) {
            return 0.0
        } else {
            return connections.mapNotNull { (it as? ComponentPinFunc)?.getValue?.invoke(t) }.sum()
        }
    }
}