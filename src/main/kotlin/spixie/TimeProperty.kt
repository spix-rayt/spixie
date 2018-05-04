package spixie

import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform

class TimeProperty(private var bpm: Double) {
    private var _frame = 0
    private var _time = 0.0

    var frame:Int
    get() = _frame
    set(value) {
        assert(Platform.isFxApplicationThread())

        _time = bpm/3600*value
        _frame = value
        timeChanges.onNext(_time)
    }

    var time:Double
    get() = _time
    set(value) {
        assert(Platform.isFxApplicationThread())

        _frame = Math.round(value*3600/bpm).toInt()
        _time = value
        timeChanges.onNext(_time)
    }

    val timeChanges = BehaviorSubject.createDefault(_time)
}