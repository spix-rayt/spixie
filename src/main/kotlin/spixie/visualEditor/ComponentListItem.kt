package spixie.visualEditor

class ComponentListItem(val clazz: Class<*>) {
    override fun toString(): String {
        return clazz.simpleName
    }
}