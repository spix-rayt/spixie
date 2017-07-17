package spixie.components

import javafx.scene.layout.VBox
import spixie.*
import java.util.*

class ParticleSpray(x:Double, y:Double) : VisualEditorComponent(x, y), SpixieHashable {
    val red = Value(0.6, 0.001, "Red", true)
    val green = Value(0.6, 0.001, "Green", true)
    val blue = Value(1.0, 0.001, "Blue", true)
    val alpha = Value(1.5, 0.001, "Aplha", true)
    val size = Value(4.0,0.1, "Size", true)
    val beamParticlesCount = Value(1.0, 1.0, "Beam particles count", true)
    val beamRate = Value(1.0, 1.0, "Beam rate", true)
    val startSpeedMin = Value(0.0, 1.0, "Start speed MIN", true)
    val startSpeedMax = Value(150.0, 1.0, "Start speed MAX", true)
    val coefDeceleration = Value(0.997, 0.0001, "Deceleration coefficient", true)
    val brownianMotion = Value(20.0, 0.2, "Brownian motion", true)

    init {
        val vBox = VBox()
        children.addAll(vBox)
        vBox.children.addAll(red, green, blue, alpha, size, beamParticlesCount, beamRate, startSpeedMin, startSpeedMax, coefDeceleration, brownianMotion)
    }

    var frameParticles = 0
    var particles = ArrayList<Particle>()

    fun clearParticles(){
        frameParticles = -1
        particles.clear()
    }

    fun stepParticles(){
        frameParticles++
        val delta = frameToTime(1, Main.world.bpm.get())
        var i=0L
        for (particle in particles) {
            particle.vx*=coefDeceleration.get().toFloat()
            particle.vy*=coefDeceleration.get().toFloat()
            val angle = rand(0,0,0,frameParticles.toLong(),i,5)*Math.PI*2
            val speed = rand(0,0,0,frameParticles.toLong(),i,6)*brownianMotion.get()
            particle.vx += (Math.cos(angle) * speed).toFloat()
            particle.vy += (Math.sin(angle) * speed).toFloat()
            particle.step(delta)
            i++
        }
        if(frameParticles % beamRate.get().toInt() == 0){
            for(q in 0..(beamParticlesCount.get().toLong()-1)){
                val particle = Particle()
                particle.red = red.get().toFloat()
                particle.green = green.get().toFloat()
                particle.blue = blue.get().toFloat()
                particle.alpha = alpha.get().toFloat()

                particle.size = size.get().toFloat()
                val angle = rand(0,0,0,frameParticles.toLong(),q,0)*Math.PI*2
                val speed = rand(0,0,0,frameParticles.toLong(),q,1)*(startSpeedMax.get() - startSpeedMin.get()) + startSpeedMin.get()
                particle.vx = (Math.cos(angle) * speed).toFloat()
                particle.vy = (Math.sin(angle) * speed).toFloat()
                particle.x = 0.0f
                particle.y = 0.0f
                particles.add(particle)
            }
        }
    }

    override fun spixieHash(): Long {
        return red.get().raw() mix
                green.get().raw() mix
                blue.get().raw() mix
                alpha.get().raw() mix
                size.get().raw() mix
                beamParticlesCount.get().raw() mix
                beamRate.get().raw() mix
                startSpeedMin.get().raw() mix
                startSpeedMax.get().raw() mix
                coefDeceleration.get().raw() mix
                brownianMotion.get().raw()
    }
}
