package spixie

class RootTimeChanger(private val frame: Value, private val bpm: Value, private val time: Value) : ValueChanger {

    override fun updateOutValue() {
        time.set(bpm.fraction.divide(3600).multiply(frame.fraction))
    }

    override val valueToBeChanged: Value.Item
        get() = Value.EMPTY.item()
}
