package spixie

interface ValueChanger {
    fun updateOutValue()
    val valueToBeChanged: Value.Item
}
