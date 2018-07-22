package bounces

import bounces.Physics.linear
import org.apache.commons.math3.util.FastMath
import java.lang.IllegalArgumentException

sealed class Line() {
    abstract fun rotate(radians: Double, center: Vector): Line
}

data class StraightLine(val from: Cartesian, val to: Cartesian) : Line() {
    override fun rotate(radians: Double, center: Vector) = StraightLine(
            (from - center).rotate(radians) + center,
            (to - center).rotate(radians) + center)

    val direction by lazy { (to - from).asPolar().d }
    val isVertical by lazy {
        FastMath.abs(this.direction % Math.PI - Math.PI / 2) < Vector.PRECISION
                || FastMath.abs(this.direction % Math.PI + Math.PI / 2) < Vector.PRECISION
    }
    val yFunction by lazy {
        if (!isVertical) linear(this)
        else throw IllegalArgumentException("Not applicable")
    }
    private val left = if (from.x < to.x) from else if (to.x < from.x) to else if (from.y < to.y) from else to
    private val right = if (from == left) to else from
    fun contains(point: Vector): Boolean {
        val cartesian = point.asCartesian()
        return if (isVertical) {
            cartesian.x == from.x && left.y <= cartesian.y && cartesian.y <= right.y
        } else {
            left.x <= cartesian.x && cartesian.x <= right.x && FastMath.abs(yFunction(cartesian.x) - cartesian.y) <= Vector.PRECISION
        }
    }

    fun findIntersection(other: StraightLine): Vector? {
        return if (isOtherLineCrossesThis(other) && other.isOtherLineCrossesThis(this)) {
            listOf(getStraightsIntersectionPoint(other))
                    .firstOrNull { this.contains(it) && other.contains(it) }
        } else {
            null
        }
    }

    private fun isOtherLineCrossesThis(other: StraightLine): Boolean {
        val product1 = (this.to - this.from).cross(other.from - this.from)
        val product2 = (this.to - this.from).cross(other.to - this.from)
        return product1 * product2 < 0
    }

    private fun getStraightsIntersectionPoint(other: StraightLine): Vector {
        return when {
            this.isVertical -> Cartesian(this.from.x, other.yFunction(this.from.x))
            other.isVertical -> Cartesian(other.from.x, this.yFunction(other.from.x))
            else -> {
                val x = (other.yFunction(0.0) - this.yFunction(0.0)) / (FastMath.tan(this.direction) - FastMath.tan(other.direction))
                Cartesian(x, this.yFunction(x))
            }
        }
    }

    fun distanceTo(point: Vector): Double {
        val normal = (to - from).normal()
        val normalThroughPoint = StraightLine(point.asCartesian(), (point + normal).asCartesian())
        return (getStraightsIntersectionPoint(normalThroughPoint) - point).module()
    }
}
