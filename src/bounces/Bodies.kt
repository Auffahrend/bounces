package bounces

import org.apache.commons.math3.util.FastMath.PI

interface Body {
    val mass: Double
    var speed: Vector
}

data class Wall(var from: Cartesian, var size: Cartesian) : Body {
    override val mass = Double.POSITIVE_INFINITY
    override var speed: Vector
        get() = Polar.ZERO
        set(value) {}
}

sealed class Movable(open var center: Cartesian, override var speed: Vector,
                     open var turn: Polar, open var angularSpeed: Double) : Body

data class Circle(val radius: Double, override var center: Cartesian, override var speed: Vector,
                  override var turn: Polar, override var angularSpeed: Double)
    : Movable(center, speed, turn, angularSpeed) {
    override val mass = 4 * PI * radius * radius

}

data class Rect(val size: Cartesian, override var center: Cartesian, override var speed: Vector,
                override var turn: Polar, override var angularSpeed: Double)
    : Movable(center, speed, turn, angularSpeed) {
    override val mass = size.x * size.y
}
