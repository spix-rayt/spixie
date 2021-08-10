package spixie.visualEditor.pins

import spixie.visualEditor.ParticleArray

class ComponentPinParticleArray(name: String): ComponentPin(name) {
    var getValue: (() -> ParticleArray)? = null

    fun receiveValue(): ParticleArray {
        val particleArrays = connections
                .sortedBy { it.editorComponent.layoutY }
                .mapNotNull { (it as? ComponentPinParticleArray)?.getValue?.invoke() }
        val resultArray = particleArrays.flatMap { it.array }
        return ParticleArray(resultArray, resultArray.size.toFloat() + particleArrays.sumByDouble { it.decimalSize.toDouble() }.rem(1.0).toFloat())
    }
}