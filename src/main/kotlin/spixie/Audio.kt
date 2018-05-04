package spixie

import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import spixie.static.runInUIAndWait
import java.io.DataInputStream
import java.io.File
import javax.sound.sampled.AudioSystem

class Audio {
    private var ready = false
    private var play = false
    var rms = FloatArray(0)

    private var media:Media? = null
    private var mediaPlayer:MediaPlayer? = null

    fun play(duration: Duration){
        mediaPlayer?.let {
            if(ready){
                it.seek(duration)
                it.play()
                play = true
            }
        }
    }

    fun pause(){
        play = false
        mediaPlayer?.let {
            it.pause()
        }
    }

    fun isPlaying(): Boolean {
        return play
    }

    fun getTime(): Double{
        mediaPlayer?.let {
            return it.currentTime.toSeconds()
        }
        return 0.0
    }

    fun load(file:File){
        Thread(Runnable {
            val audioInputStream = AudioSystem.getAudioInputStream(file)
            val dataInputStream = DataInputStream(audioInputStream.buffered())
            val blockSize = audioInputStream.format.frameRate.toInt()/100
            val rmsBlocks = ArrayList<Float>()
            var rmsSumm = 0.0
            val length = audioInputStream.frameLength
            var i = 0
            kotlin.run {
                do {
                    for(j in 0 until blockSize){
                        var max = Short.MIN_VALUE
                        for(k in 0 until audioInputStream.format.channels){
                            val s = dataInputStream.readShort()
                            if(s>max) max = s
                        }
                        val s2 = (max+32768)/65535.0f*2-1
                        rmsSumm+=s2*s2
                        i+=1
                        if(i >= length) return@run
                    }
                    rmsBlocks.add(Math.sqrt(rmsSumm/blockSize).toFloat())
                    rmsSumm = 0.0
                } while (true)
            }


            rms = rmsBlocks.toFloatArray()
            Main.workingWindow.arrangementWindow.needRedrawWaveform = true


            try {
                mediaPlayer?.let { it.stop() }
                media = Media(file.toURI().toString())
                mediaPlayer = MediaPlayer(media)
                ready=false
                mediaPlayer?.let {
                    it.setOnReady {
                        ready = true
                    }
                    it.setOnEndOfMedia {
                        pause()
                    }
                }
            }catch (e:Exception){
                media = null
                mediaPlayer = null
                ready = false
                e.printStackTrace()
            }
        }).start()
    }
}