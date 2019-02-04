package spixie.visualEditor

class ComponentPinParticleArray(component: Component, val getValue: (() -> ParticleArray)?, name: String): ComponentPin(component, name) {
    fun receiveValue(): ParticleArray{
        val allConnections = (connections + imaginaryConnections).toSet()
        val particleArrays = allConnections
                .sortedBy { it.component.layoutY }
                .mapNotNull { (it as? ComponentPinParticleArray)?.getValue?.invoke() }
        val resultArray = particleArrays.flatMap { it.array }
        return ParticleArray(resultArray, resultArray.size.toFloat() + particleArrays.sumByDouble { it.decimalSize.toDouble() }.rem(1.0).toFloat())
    }
}