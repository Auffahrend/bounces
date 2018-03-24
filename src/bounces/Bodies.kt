package bounces

import org.apache.commons.math3.util.FastMath
import org.apache.commons.math3.util.FastMath.PI

interface Body

data class Wall(var from: Cartesian, var size: Cartesian, val inside: Vector) : Body

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

    val innerY1Function : (Double) -> Double = { x -> FastMath.sqrt(radius * radius - x * x) }
    val innerY2Function : (Double) -> Double = { x -> -FastMath.sqrt(radius * radius - x * x) }
    fun contains(point: Vector): Boolean {
        return point.asCartesian().x in center.x- FastMath.abs(radius).. center.x+ FastMath.abs(radius)
                && (FastMath.abs(innerY1Function(point.asCartesian().x - center.x) - (point.asCartesian().y - center.y)) <= Vector.PRECISION
                || FastMath.abs(innerY2Function(point.asCartesian().x - center.x) - (point.asCartesian().y - center.y)) <= Vector.PRECISION)
    }
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

    fun lines(): List<StraightLine> = listOf(StraightLine(p1, p2), StraightLine(p2, p3), StraightLine(p3, p4), StraightLine(p4, p1))
}
