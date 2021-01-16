package spixie.raymarching.geometryobject

class Sphere : GeometryObject() {
    var x: Float
        get() { return floats[0] }
        set(value) { floats[0] = value }

    var y: Float
        get() { return floats[1] }
        set(value) { floats[1] = value }

    var z: Float
        get() { return floats[2] }
        set(value) { floats[2] = value }

    var radius: Float
        get() { return floats[3] }
        set(value) { floats[3] = value }
}