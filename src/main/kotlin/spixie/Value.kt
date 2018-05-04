package spixie

import io.reactivex.subjects.PublishSubject

class Value<T>(initial:T) {
    var value:T = initial
        set(value) {
            field = value
            changes.onNext(value)
        }

    val changes = PublishSubject.create<T>()
}