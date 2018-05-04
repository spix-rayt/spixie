package spixie.visual_editor

class ComponentListItem(val clazz: Class<*>) {
    override fun toString(): String {
        return clazz.simpleName
    }
}