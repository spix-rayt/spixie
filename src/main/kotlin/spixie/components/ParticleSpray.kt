package spixie.components

import javafx.scene.layout.VBox
import spixie.*
import java.util.*

class ParticleSpray(x:Double, y:Double) : VisualEditorComponent(x, y), SpixieHashable {
    val red = ValueControl(1.0, 0.001, "Red")
    val green = ValueControl(1.0, 0.001, "Green")
    val blue = ValueControl(0.0, 0.001, "Blue")
    val alpha = ValueControl(1.0, 0.001, "Aplha")
    val size = ValueControl(1.0,0.1, "Size")
    val beamParticlesCount = ValueControl(10.0, 1.0, "Beam particles count")
    val beamRate = ValueControl(1.0, 1.0, "Beam rate")
    val startSpeedMin = ValueControl(0.0, 1.0, "Start speed MIN")
    val startSpeedMax = ValueControl(0.0, 1.0, "Start speed MAX")
    val coefDeceleration = ValueControl(1.0, 0.0001, "Deceleration coefficient")
    val brownianMotion = ValueControl(1.0, 0.05, "Brownian motion")

    init {
        val vBox = VBox()
        children.addAll(vBox)
        vBox.children.addAll(red, green, blue, alpha, size, beamParticlesCount, beamRate, startSpeedMin, startSpeedMax, coefDeceleration, brownianMotion)

        red.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        green.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        blue.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        alpha.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        size.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        beamParticlesCount.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        beamRate.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        startSpeedMin.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        startSpeedMax.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        coefDeceleration.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
        brownianMotion.onInputOutputConnected = { a, b -> onValueInputOutputConnected(a, b) }
    }

    var frameParticles = 0
    var particles = ArrayList<Particle>()

    fun clearParticles(){
        frameParticles = -1
        particles.clear()
    }

    fun stepParticles(){
        frameParticles++
        val delta = frameToTime(1, Main.world.bpm.value.value)
        var i=0L
        for (particle in particles) {
            particle.vx*=coefDeceleration.value.value.toFloat()
            particle.vy*=coefDeceleration.value.value.toFloat()
            val angle = rand(0,0,0,frameParticles.toLong(),i,5)*Math.PI*2
            val speed = rand(0,0,0,frameParticles.toLong(),i,6)*brownianMotion.value.value
            particle.vx += (Math.cos(angle) * speed).toFloat()
            particle.vy += (Math.sin(angle) * speed).toFloat()
            particle.step(delta)
            i++
        }
        if(frameParticles % beamRate.value.value.toInt() == 0){
            for(q in 0..(beamParticlesCount.value.value.toLong()-1)){
                val particle = Particle()
                particle.red = red.value.value.toFloat()
                particle.green = green.value.value.toFloat()
                particle.blue = blue.value.value.toFloat()
                particle.alpha = alpha.value.value.toFloat()

                particle.size = size.value.value.toFloat()
                val angle = rand(0,0,0,frameParticles.toLong(),q,0)*Math.PI*2
                val speed = rand(0,0,0,frameParticles.toLong(),q,1)*(startSpeedMax.value.value - startSpeedMin.value.value) + startSpeedMin.value.value
                particle.vx = (Math.cos(angle) * speed).toFloat()
                particle.vy = (Math.sin(angle) * speed).toFloat()
                particle.x = 0.0f
                particle.y = 0.0f
                particles.add(particle)
            }
        }
    }

    override fun spixieHash(): Long {
        return red.value.value.raw() mix
                green.value.value.raw() mix
                blue.value.value.raw() mix
                alpha.value.value.raw() mix
                size.value.value.raw() mix
                beamParticlesCount.value.value.raw() mix
                beamRate.value.value.raw() mix
                startSpeedMin.value.value.raw() mix
                startSpeedMax.value.value.raw() mix
                coefDeceleration.value.value.raw() mix
                brownianMotion.value.value.raw()
    }

    var onValueInputOutputConnected: (Any, Any) -> Unit = { _, _ ->  }
}
