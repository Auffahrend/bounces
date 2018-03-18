package bounces

import org.apache.commons.math3.util.FastMath.PI

interface Body

data class Wall(var from: Cartesian, var size: Cartesian) : Body

sealed class Movable(open var center: Cartesian, open var speed: Vector,
                     open var turn: Polar, open var angularSpeed: Double) : Body {
    abstract val mass: Double
    abstract val inertia: Double
    open fun move(time: Double) {
        center += speed * time
        turn.rotate(angularSpeed * time)
    }
}

data class Circle(val radius: Double, override var center: Cartesian, override var speed: Vector,
                  override var turn: Polar, override var angularSpeed: Double)
    : Movable(center, speed, turn, angularSpeed) {
    override val mass = 4 * PI * radius * radius
    override val inertia = mass * radius * radius / 2
}

data class Rect(val size: Cartesian, override var center: Cartesian, override var speed: Vector,
                override var turn: Polar, override var angularSpeed: Double)
    : Movable(center, speed, turn, angularSpeed) {
    override val mass = size.x * size.y
    override val inertia =  size.moduleSqr() * 4 / 3
    lateinit var p1 : Cartesian
    lateinit var p2 : Cartesian
    lateinit var p3 : Cartesian
    lateinit var p4 : Cartesian

    override fun move(time: Double) {
        super.move(time)
        val halfWidth = turn * (size.x / 2)
        val halfHeight = turn.normal() * (size.y / 2)
        p1 = (center + halfHeight + halfWidth).asCartesian()
        p2 = (center - halfHeight + halfWidth).asCartesian()
        p3 = (center - halfHeight - halfWidth).asCartesian()
        p4 = (center + halfHeight - halfWidth).asCartesian()
    }
}
