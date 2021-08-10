package spixie

import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform
import spixie.static.beatsToSeconds
import spixie.static.frameToBeats
import spixie.static.frameToSeconds
import spixie.static.secondsToFrame
import kotlin.math.roundToInt

class TimeHolder(private var bpm: NumberControl, private var fps: NumberControl, private var offset: NumberControl) {
    private var _frame = 0
    private var _beats = 0.0
    private var _seconds = 0.0

    var frame: Int
        get() = _frame
        set(value) {
            assert(Platform.isFxApplicationThread())

            _beats = frameToBeats(value, bpm.value, fps.value.roundToInt())
            _frame = value
            _seconds = frameToSeconds(value, fps.value.roundToInt())
            timeChanged()
        }

    var beats: Double
        get() = _beats
        set(value) {
            assert(Platform.isFxApplicationThread())

            _beats = value
            _seconds = beatsToSeconds(_beats, bpm.value)
            _frame = secondsToFrame(_seconds, fps.value.roundToInt())
            timeChanged()
        }

    var seconds: Double
        get() = _seconds
        set(value) {
            assert(Platform.isFxApplicationThread())

            _seconds = value
            _frame = secondsToFrame(_seconds, fps.value.roundToInt())
            _beats = (frameToBeats(_frame, bpm.value, fps.value.roundToInt()) + offset.value).coerceAtLeast(0.0)

            timeChanged()
        }

    private fun timeChanged() {
        timeChanges.onNext(_beats)
    }

    val timeChanges = BehaviorSubject.createDefault(_beats)
}