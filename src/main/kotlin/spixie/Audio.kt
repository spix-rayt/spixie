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
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.roundToInt

class Audio {
    @Volatile private var play = false

    @Volatile var spectra = listOf<DoubleArray>()

    @Volatile private var spectraStep = 1.0

    @Volatile private var frequencyStep = 1.0

    @Volatile private var mediaPlayer:MediaPlayer? = null

    private val stopWatch = StopWatch()

    private var stopWatchAdd = 0.0

    fun play(duration: Duration) {
        val mediaPlayer = mediaPlayer
        if(mediaPlayer != null) {
            play = true
            mediaPlayer.seek(duration)
            mediaPlayer.play()
        }
        stopWatch.reset()
        stopWatchAdd = duration.toMillis()
        stopWatch.start()
    }

    fun pause() {
        play = false
        mediaPlayer?.pause()
        if(stopWatch.isStarted) {
            stopWatch.stop()
        }
    }

    fun isPlaying(): Boolean {
        return stopWatch.isStarted
    }

    fun getTime(): Double{
        val mediaPlayer = mediaPlayer
        if(mediaPlayer != null && play) {
            return mediaPlayer.currentTime.toSeconds()
        } else {
            return (stopWatch.time + stopWatchAdd)/1000.0
        }
    }

    fun load(file:File) {
        mediaPlayer?.stop()
        mediaPlayer = null
        spectra = listOf()
        timelineWindow.spectrogram.requestRedraw()
        thread {
            println("Audio: start loading")
            val exitValue = if (file.canonicalPath == File("audio.aiff").canonicalPath) {
                0
            } else {
                ProcessBuilder(
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
            }
            println("Audio: ffmpeg converting finished with code $exitValue")

            if(exitValue == 0) {
                println("Audio: preparing for FFT")
                val audioInputStream = AudioSystem.getAudioInputStream(File("audio.aiff"))
                val dataInputStream = DataInputStream(audioInputStream.buffered())
                val halfBlockSize = 1024 * 16
                val blockSize = halfBlockSize * 2
                val blockStepSize = audioInputStream.format.frameRate.toInt() / 100

                val windowFunction = (-halfBlockSize until halfBlockSize).map { n->
                    0.54f + 0.46f * cos(n * Math.PI / halfBlockSize)
                }.toTypedArray()

//                val windowFunction = (-halfBlockSize until halfBlockSize).map { n->
//                    cos(n * Math.PI / halfBlockSize)
//                }.toTypedArray()

//                val hammingWindow = (-(halfBlockSize) until (halfBlockSize)).map { n->
//                    if(n < 0) {
//                        0.0
//                    } else {
//                        0.54f + 0.46f * cos(n * Math.PI / (halfBlockSize))
//                    }
//                }.toTypedArray()

                val windowFunctionSum = windowFunction.sum()
                println("Audio: windowFunctionSum = $windowFunctionSum")

                val spectraResult = arrayListOf<DoubleArray>()
                try {
                    var amplitudes = DoubleArray(halfBlockSize) { 0.0 } + DoubleArray(halfBlockSize) {
                        val c1 = dataInputStream.readShort()
                        val c2 = dataInputStream.readShort()
                        (c1 + 32768.0) / 65535.0 * 2.0 - 1.0
                    }

                    println("Audio: FFT start")
                    do {
                        val windowedAmplitudes = amplitudes.mapIndexed { index, d -> d * windowFunction[index] }.toDoubleArray()
                        val fastFourierTransformer = FastFourierTransformer(DftNormalization.STANDARD)
                        val fft = fastFourierTransformer.transform(windowedAmplitudes, TransformType.FORWARD)
                        spectraResult.add(fft.slice(0..(blockSize / 16)).map { it.abs() / windowFunctionSum }.toDoubleArray())
                        amplitudes = amplitudes.sliceArray(blockStepSize until blockSize) + DoubleArray(blockStepSize) {
                            val c1 = dataInputStream.readShort()
                            val c2 = dataInputStream.readShort()
                            (c1 + 32768.0) / 65535.0 * 2.0 - 1.0
                        }
                    } while (true)
                }catch (e: EOFException) {
                }

                println("Audio: FFT end")

                val max = spectraResult.map { it.maxOrNull() ?: 0.0 }.maxOrNull() ?: 0.0
                val min = (spectraResult.map { it.minOrNull() ?: 0.0 }.minOrNull() ?: 0.0)
                println("Audio: max = $max")
                println("Audio: min = $min")

                spectra = spectraResult.map {
                    it.map { v ->
                        if(v < Double.MIN_VALUE) {
                            0.0
                        } else {
                            ((10.0 * log10(v / max) + 1.0).coerceIn(-24.0, 0.0) + 24.0) / 24.0
                        }
                    }.toDoubleArray()
                }
                spectraStep = blockStepSize / 44100.0
                frequencyStep = 44100.0 / blockSize.toDouble()
                println("Audio: frequency step = $frequencyStep")
                println("Audio: max frequency = ${(spectraResult[0].lastIndex) * frequencyStep}")
                timelineWindow.spectrogram.requestRedraw()


                println("Audio: loading MediaPlayer")
                try {
                    val newMediaPlayer = MediaPlayer(Media(File("audio.aiff").toURI().toString()))
                    newMediaPlayer.let {
                        it.setOnReady {
                            println("Audio: MediaPlayer is ready")
                            mediaPlayer = newMediaPlayer
                            if(stopWatch.isStarted) {
                                play(Duration(stopWatch.time + stopWatchAdd))
                            }
                        }
                        it.setOnEndOfMedia {
                            if(mediaPlayer == it) {
                                play = false
                            }
                        }
                    }
                } catch (e:Exception) {
                    mediaPlayer = null
                    e.printStackTrace()
                }
            }
        }
    }

    fun getAmplitude(time: Double, frequency: Double): Double {
        val timeIndex = (time / spectraStep).roundToInt()
        val frequencyIndex = (frequency / frequencyStep).roundToInt()
        if(timeIndex !in spectra.indices) {
            return 0.0
        }
        val spectraColumn = spectra[timeIndex]
        if(frequencyIndex !in spectraColumn.indices) {
            return 0.0
        }

        return spectraColumn[frequencyIndex]
    }
}