package spixie.visual_editor.components

import spixie.visual_editor.Particle
import spixie.visual_editor.Component
import spixie.visual_editor.ComponentPin
import spixie.visual_editor.ParticleArray

class Test: Component() {
    val inRadius = ComponentPin(this, null, "Radius",Double::class.java)
    val inPhase = ComponentPin(this, null, "Phase", Double::class.java)
    val inSize = ComponentPin(this, null, "Size", Double::class.java)
    val inCount = ComponentPin(this, null, "Count", Double::class.java)


    val outParticles = ComponentPin(this, {
        val radius = inRadius.receiveValue()
        val phase = inPhase.receiveValue()
        val size = inSize.receiveValue()
        val count = inCount.receiveValue()

        val particles = ArrayList<Particle>()

        if (radius != null && phase != null && size != null && count != null) {
            var i = 0
            while (i < count) {
                particles.add(
                        Particle().apply {
                            x = (Math.cos(Math.PI * 2 / count * i + phase * Math.PI * 2.0) * radius).toFloat()
                            y = (Math.sin(Math.PI * 2 / count * i + phase * Math.PI * 2.0) * radius).toFloat()
                            this.size = size.toFloat()
                        }
                )
                i++
            }
        }
        ParticleArray(particles)
    }, "Particles", ParticleArray::class.java)

    init {
        inputPins.add(inRadius)
        inputPins.add(inPhase)
        inputPins.add(inSize)
        inputPins.add(inCount)
        outputPins.add(outParticles)
        updateVisual()
    }
}