package spixie.timeline

import io.reactivex.BackpressureStrategy
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import spixie.Audio
import spixie.projectWindow
import java.awt.image.BufferedImage
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class Spectrogram(val content: Group, timelineWindow: TimelineWindow, audio: Audio): Canvas(1.0, 1080.0 / 2.0) {
    private val redrawSpectrogram = PublishSubject.create<Unit>()
    fun requestRedraw() {
        redrawSpectrogram.onNext(Unit)
    }

    init {
        redrawSpectrogram.toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation(), false, 1)
                .map {
                    val newWaveformLayoutX = -content.layoutX - 100

                    val startTime = timelineWindow.calcTimeOfX(newWaveformLayoutX) - projectWindow.offset.value
                    val endTime = timelineWindow.calcTimeOfX(newWaveformLayoutX + width) - projectWindow.offset.value

                    val startSecond = startTime * 3600 / projectWindow.bpm.value / 60
                    val endSecond = endTime * 3600 / projectWindow.bpm.value / 60

                    val secondsInPixel = (endSecond - startSecond) / this.width

                    val bufferedImage = BufferedImage(this.width.toInt(), this.height.toInt(), BufferedImage.TYPE_3BYTE_BGR)
                    val pixelArray = IntArray(bufferedImage.width * bufferedImage.height * 3)


                    for (x in 0 until bufferedImage.width) {
                        val time = startSecond + x * secondsInPixel
                        for (y in 0 until bufferedImage.height) {
                            val offset = (y * bufferedImage.width + x) * 3
                            val lowestNoteFrequency = 440.0 / 2.0 / 2.0 / 2.0 / 2.0
                            val n = 80.0 * (1.0 - (y.toDouble() / bufferedImage.height.toDouble()))
                            val frequency = lowestNoteFrequency * (2.0.pow(n / 12.0))
                            val amplitude = audio.getAmplitude(time, frequency)
                            pixelArray[offset    ] = ((1.0 - amplitude) * 255).toInt()
                            pixelArray[offset + 1] = ((1.0 - amplitude) * 255).toInt()
                            pixelArray[offset + 2] = ((1.0 - amplitude) * 255).toInt()
                        }
                    }

                    bufferedImage.raster.setPixels(0, 0, bufferedImage.width, bufferedImage.height, pixelArray)
                    Pair(newWaveformLayoutX, SwingFXUtils.toFXImage(bufferedImage, null))
                }
                .observeOn(JavaFxScheduler.platform())
                .subscribe { (newWaveformLayoutX, fxImage)->
                    this.graphicsContext2D.drawImage(fxImage, 0.0, 0.0)
                    this.layoutX = newWaveformLayoutX
                }
    }
}