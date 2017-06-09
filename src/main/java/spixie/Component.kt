package spixie

class Component(private val name: String) {
    var componentBody = ComponentBody()

    override fun toString(): String {
        return name
    }
}
