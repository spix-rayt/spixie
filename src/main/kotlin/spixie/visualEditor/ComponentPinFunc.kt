package spixie.visualEditor

class ComponentPinFunc(component: Component, val getValue: ((t: Double) -> Double)?, name: String): ComponentPin(component, name) {
    fun receiveValue(t: Double): Double {
        val allConnections = (connections + imaginaryConnections).toSet()
        if(allConnections.isEmpty()){
            return 1.0
        }else{
            return allConnections
                    .mapNotNull { (it as? ComponentPinFunc)?.getValue?.invoke(t) }.sum()
        }
    }
}