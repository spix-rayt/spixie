package spixie

import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import org.apache.commons.lang3.time.StopWatch
import spixie.static.Settings
import java.io.DataInputStream
import java.io.File
import javax.sound.sampled.AudioSystem

class Audio {
    @Volatile private var play = false
    var rms = FloatArray(0)

    @Volatile private var mediaPlayer:MediaPlayer? = null
    private val stopWatch = StopWatch()
    private var stopWatchAdd = 0.0

    fun play(duration: Duration){
        val mediaPlayer = mediaPlayer
        if(mediaPlayer != null){
            play = true
            mediaPlayer.seek(duration)
            mediaPlayer.play()
        }
        stopWatch.reset()
        stopWatchAdd = duration.toMillis()
        stopWatch.start()
    }

    fun pause(){
        play = false
        mediaPlayer?.pause()
        if(stopWatch.isStarted){
            stopWatch.stop()
        }
    }

    fun isPlaying(): Boolean {
        return stopWatch.isStarted
    }

    fun getTime(): Double{
        val mediaPlayer = mediaPlayer
        if(mediaPlayer != null && play){
            return mediaPlayer.currentTime.toSeconds()
        }else{
            return (stopWatch.time + stopWatchAdd)/1000.0
        }
    }

    fun load(file:File){
        mediaPlayer?.stop()
        mediaPlayer = null
        rms = FloatArray(0)
        Main.arrangementWindow.needRedrawWaveform = true
        Thread(Runnable {
            val exitValue = ProcessBuilder(
                    listOfNotNull(
                            Settings.ffmpeg,
                            "-y",
                            "-i", file.absolutePath,
                            "-vn",
                            "-ar", "44100",
                            "-c:a", "pcm_s16be",
                            "-f", "aiff",
                            "audio.aiff"
                    )
            ).start().waitFor()

            if(exitValue == 0){
                val audioInputStream = AudioSystem.getAudioInputStream(File("audio.aiff"))
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
                Main.arrangementWindow.needRedrawWaveform = true



                try {
                    val newMediaPlayer = MediaPlayer(Media(File("audio.aiff").toURI().toString()))
                    newMediaPlayer.let {
                        it.setOnReady {
                            mediaPlayer = newMediaPlayer
                            if(stopWatch.isStarted){
                                play(Duration(stopWatch.time + stopWatchAdd))
                            }
                        }
                        it.setOnEndOfMedia {
                            if(mediaPlayer == it){
                                play = false
                            }
                        }
                    }
                }catch (e:Exception){
                    mediaPlayer = null
                    e.printStackTrace()
                }
            }
        }).start()
    }
}