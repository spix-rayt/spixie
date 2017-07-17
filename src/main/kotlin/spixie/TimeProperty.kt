package spixie

class TimeProperty(bpm:Double) {
    private var _frame = 0
    private var _time = 0.0
    private var bpm = bpm

    var frame:Int
    get() = _frame
    set(value) {
        _time = bpm/3600*value
        _frame = value
        for (frameListener in frameListeners) {
            frameListener(_frame)
        }
        for (timeListener in timeListeners) {
            timeListener(_time)
        }
    }

    var time:Double
    get() = _time
    set(value) {
        _frame = Math.round(value*3600/bpm).toInt()
        _time = value
        for (frameListener in frameListeners) {
            frameListener(_frame)
        }
        for (timeListener in timeListeners) {
            timeListener(_time)
        }
    }

    private var frameListeners = listOf<(Int) -> Unit>()
    private var timeListeners = listOf<(Double) -> Unit>()

    fun onFrameChanged(frameChangedEvent: (frame:Int) -> Unit){
        frameListeners+=frameChangedEvent
    }

    fun onTimeChanged(timeChangedEvent: (time:Double) -> Unit){
        timeListeners+=timeChangedEvent
    }
}