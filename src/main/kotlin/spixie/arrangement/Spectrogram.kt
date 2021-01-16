package spixie.arrangement

import io.reactivex.BackpressureStrategy
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import spixie.Core
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

class Spectrogram(val content: Group): Canvas(1.0, 300.0) {
    private val redrawSpectrogram = PublishSubject.create<Unit>()
    fun requestRedraw(){
        redrawSpectrogram.onNext(Unit)
    }

    init {
        redrawSpectrogram.toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.newThread(), false, 1)
                .map {
                    val newWaveformLayoutX = -content.layoutX - 100

                    val startTime = Core.arrangementWindow.calcTimeOfX(newWaveformLayoutX) - Core.renderManager.offset.value
                    val endTime = Core.arrangementWindow.calcTimeOfX(newWaveformLayoutX + width) - Core.renderManager.offset.value

                    val startSecond = startTime * 3600 / Core.renderManager.bpm.value / 60
                    val endSecond = endTime * 3600 / Core.renderManager.bpm.value / 60

                    val secondsInPixel = (endSecond - startSecond) / this.width

                    val bufferedImage = BufferedImage(this.width.toInt(), this.height.toInt(), BufferedImage.TYPE_3BYTE_BGR)
                    val pixelArray = IntArray(bufferedImage.width * bufferedImage.height * 3)

                    val spectra = Core.audio.spectra
                    for (x in 0 until bufferedImage.width) {
                        val time = ((startSecond + x * secondsInPixel) * 100).roundToInt()
                        var prevT = 0
                        for (y in 0 until bufferedImage.height) {
                            val offset = (y * bufferedImage.width + x) * 3
                            if (time >= 0 && time < spectra.size && spectra.size > 2) {
                                //val t = ((1.0 - (y.toDouble() / bufferedImage.height)).pow(1.7) * 0.99 + 0.01).coerceIn(0.0, 1.0) * (spectra[0].size - 2)
                                val t = (Math.pow(2.0, (60+((299-y)/299.0*74.0)) / 12.0) / (44100 / 8192.0)).roundToInt()
                                val v = spectra[time].slice(t..prevT).max() ?: 0.0
                                val vv = ((1.0 - v).coerceIn(0.0, 1.0)*20).toInt()/20.0
                                pixelArray[offset    ] = (vv*(255-16) + 16).toInt()
                                pixelArray[offset + 1] = (vv*(255-27) + 27).toInt()
                                pixelArray[offset + 2] = (vv*(255-44) + 44).toInt()
                                prevT = t
                            } else {
                                pixelArray[offset    ] = 255
                                pixelArray[offset + 1] = 255
                                pixelArray[offset + 2] = 255
                            }
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