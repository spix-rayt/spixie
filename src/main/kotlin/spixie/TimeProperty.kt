package spixie

import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform
import spixie.static.frameToTime

class TimeProperty(private var bpm: NumberControl) {
    private var _frame = 0
    private var _time = 0.0

    var frame:Int
    get() = _frame
    set(value) {
        assert(Platform.isFxApplicationThread())

        _time = frameToTime(value, bpm.value)
        _frame = value
        timeChanges.onNext(_time)
    }

    var time:Double
    get() = _time
    set(value) {
        assert(Platform.isFxApplicationThread())

        _frame = Math.round(value*3600/bpm.value).toInt()
        _time = value
        timeChanges.onNext(_time)
    }

    val timeChanges = BehaviorSubject.createDefault(_time)
}