package spixie.visualEditor

class ComponentPinParticleArray(component: Component, getValue: (() -> ParticleArray)?, name: String): ComponentPin(component, getValue, name) {
    override fun receiveValue(): ParticleArray{
        val allConnections = (connections + imaginaryConnections).toSet()
        val particleArrays = allConnections
                .sortedBy { it.component.layoutY }
                .mapNotNull { (it.getValue?.invoke() as? ParticleArray) }
        val resultArray = particleArrays.flatMap { it.array }
        return ParticleArray(resultArray, resultArray.size.toFloat() + particleArrays.sumByDouble { it.decimalSize.toDouble() }.rem(1.0).toFloat())
    }
}