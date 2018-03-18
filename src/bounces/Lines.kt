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

    val direction by lazy { (to-from).asPolar().d }
    val isVertical by lazy {
        FastMath.abs(this.direction % Math.PI - Math.PI / 2) < Vector.PRECISION
                || FastMath.abs(this.direction % Math.PI + Math.PI / 2) < Vector.PRECISION }
    val yFunction by lazy {
        if (!isVertical) linear(this)
        else throw IllegalArgumentException("Not applicable") }
    val left = if (from.x < to.x) from else if (to.x < from.x) to else if (from.y < to.y) from else to
    val right = if (from == left) to else from
    fun contains(point: Vector): Boolean {
        val cartesian = point.asCartesian()
        if (isVertical) {
            return cartesian.x == from.x && left.y <= cartesian.y && cartesian.y <= right.y
        } else {
            return left.x <= cartesian.x && cartesian.x <= right.x && FastMath.abs(yFunction(cartesian.x) - cartesian.y) <= Vector.PRECISION
        }
    }
}
