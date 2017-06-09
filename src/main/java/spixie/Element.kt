package spixie

interface Element {
    val values: Array<Value.Item>
    fun addGraph(outValue: Value)
}
