package bounces

import org.apache.commons.math3.util.FastMath
import org.apache.commons.math3.util.FastMath.PI

interface Body {
    val mass: Double
    val inertia: Double
    var speed: Vector
    var angularSpeed: Double
    var collisions: Int
}

data class Wall(var from: Cartesian, var size: Cartesian, val inside: Vector) : Body {
    override var collisions: Int = 0
    override val mass = Double.POSITIVE_INFINITY
    override val inertia = Double.POSITIVE_INFINITY
    override var speed: Vector
        get() = Cartesian.ZERO
        set(_) {}
    override var angularSpeed: Double
        get() = 0.0
        set(_) {}
}

sealed class Movable(open var center: Cartesian, override var speed: Vector,
                     open var turn: Polar, override var angularSpeed: Double,
                     override val mass: Double) : Body {
    override var collisions: Int = 0
    open fun move(time: Double) {
        center += speed * time
        turn = turn.rotate(angularSpeed * time)
    }

    fun speedAt(point: Vector) : Vector {
        val relativeVector = center - point
        return speed + relativeVector.normal() * angularSpeed
    }
}

data class Circle(val radius: Double, override var center: Cartesian, override var speed: Vector,
                  override var turn: Polar, override var angularSpeed: Double, override val mass: Double)
    : Movable(center, speed, turn, angularSpeed, mass) {
    override val inertia = mass * radius * radius / 2

    val innerY1Function: (Double) -> Double = { x -> FastMath.sqrt(radius * radius - x * x) }
    val innerY2Function: (Double) -> Double = { x -> -FastMath.sqrt(radius * radius - x * x) }
    fun contains(point: Vector): Boolean {
        return point.asCartesian().x in center.x - FastMath.abs(radius)..center.x + FastMath.abs(radius)
                && (FastMath.abs(innerY1Function(point.asCartesian().x - center.x) - (point.asCartesian().y - center.y)) <= Vector.PRECISION
                || FastMath.abs(innerY2Function(point.asCartesian().x - center.x) - (point.asCartesian().y - center.y)) <= Vector.PRECISION)
    }
}

data class Rect(val size: Cartesian, override var center: Cartesian, override var speed: Vector,
                override var turn: Polar, override var angularSpeed: Double, override val mass: Double)
    : Movable(center, speed, turn, angularSpeed, mass) {
    override val inertia = mass / 12 * size.module()
    lateinit var p1: Cartesian
    lateinit var p2: Cartesian
    lateinit var p3: Cartesian
    lateinit var p4: Cartesian

    override fun move(time: Double) {
        super.move(time)
        val halfWidth = turn * (size.x / 2)
        val halfHeight = turn.normal() * (size.y / 2)
        p1 = (center + halfHeight + halfWidth).asCartesian()
        p2 = (center - halfHeight + halfWidth).asCartesian()
        p3 = (center - halfHeight - halfWidth).asCartesian()
        p4 = (center + halfHeight - halfWidth).asCartesian()
    }

    fun edges(): List<StraightLine> = listOf(StraightLine(p1, p2), StraightLine(p2, p3), StraightLine(p3, p4), StraightLine(p4, p1))
    fun vertices(): List<Vector> = listOf(p1, p2, p3, p4)
}
