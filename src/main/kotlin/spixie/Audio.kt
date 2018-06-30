package spixie

import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import org.apache.commons.lang3.time.StopWatch
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import spixie.static.Settings
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import javax.sound.sampled.AudioSystem

class Audio {
    @Volatile private var play = false
    @Volatile var spectra = listOf<DoubleArray>()

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
        spectra = listOf()
        Main.arrangementWindow.spectrogram.requestRedraw()
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

                val spectraResult = arrayListOf<DoubleArray>()
                try {
                    var amplitudes = DoubleArray(8192, {
                        val c1 = dataInputStream.readShort()
                        val c2 = dataInputStream.readShort()
                        (c1+32768.0)/65535.0*2.0-1.0
                    })

                    val hammingWindow = (-4096 until 4096).map { n->
                        0.54f + 0.46f * Math.cos(n*Math.PI/4096.0)
                    }.toTypedArray()
                    do {
                        val windowedAmplitudes = amplitudes.mapIndexed { index, d -> d*hammingWindow[index] }.toDoubleArray()
                        val fastFourierTransformer = FastFourierTransformer(DftNormalization.STANDARD)
                        val fft = fastFourierTransformer.transform(windowedAmplitudes, TransformType.FORWARD)
                        spectraResult.add(fft.slice(0..450).map { it.abs() }.toDoubleArray())
                        amplitudes = amplitudes.sliceArray(blockSize until 8192) + DoubleArray(blockSize, {
                            val c1 = dataInputStream.readShort()
                            val c2 = dataInputStream.readShort()
                            (c1+32768.0)/65535.0*2.0-1.0
                        })
                    }while (true)
                }catch (e: EOFException){
                }


                val max = spectraResult.map { it.max() ?: 0.0 }.max() ?: 0.0
                val min = (spectraResult.map { it.min() ?: 0.0 }.min() ?: 0.0).coerceAtLeast(0.00000000001)
                val diff = Math.log10(max / min)

                spectra = spectraResult.map {
                    it.map {
                        if(it<0.00000000001){
                            0.0
                        }else{
                            Math.pow(((Math.log10(it / min) / diff).coerceAtMost(1.0)), 6.0)
                        }
                    }.toDoubleArray()
                }
                Main.arrangementWindow.spectrogram.requestRedraw()


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