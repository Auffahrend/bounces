package bounces

import org.apache.commons.math3.util.FastMath.PI
import org.apache.commons.math3.util.FastMath.abs
import org.apache.commons.math3.util.FastMath.atan
import org.apache.commons.math3.util.FastMath.cos
import org.apache.commons.math3.util.FastMath.hypot
import org.apache.commons.math3.util.FastMath.pow
import org.apache.commons.math3.util.FastMath.sin


interface Vector {

    fun asPolar(): Polar
    fun asCartesian(): Cartesian
    operator fun plus(v: Vector): Vector
    operator fun minus(v: Vector): Vector
    operator fun unaryMinus(): Vector
    operator fun times(k: Double): Vector
    operator fun div(k: Double): Vector
    fun rotate(radians: Double): Vector
    fun normal(): Vector
    fun module(): Double
    fun moduleSqr(): Double

    /** scalar product, a * b  */
    fun dot(v: Vector): Double

    /** vector product a x b
     * @return Since we are using 2D vectors, the abs(result) == a*b*sin(a^b),
     * and result > 0 if direction of turning a towards b is the same as direction of x-axis toward y-axis, and vise versa
     */
    fun cross(v: Vector): Double

    companion object {
        val PRECISION = 0.000_000_000_1
    }
}

class Cartesian(val x: Double, val y: Double) : Vector {

    internal var polar : Polar? = null
    internal var normal : Cartesian? = null

    override fun asPolar(): Polar {
        if (polar == null) {
            var d: Double
            if (x != 0.0) {
                d = atan(y / x)
                if (d < 0) {
                    d += PI
                } else if (y == 0.0 && x < 0) {
                    d += PI
                }
                if (y < 0) {
                    d += PI
                }
            } else if (y != 0.0) {
                d = if (y > 0) PI / 2 else 3 * PI / 2
            } else {
                d = 0.0
            }
            polar = Polar(hypot(x, y), d)
            polar!!.cartesian = this
        }
        return polar!!
    }

    override fun asCartesian() : Cartesian = this

    override fun plus(v: Vector) : Cartesian = addCartesian(v.asCartesian())

    internal fun addCartesian(v: Cartesian) : Cartesian = Cartesian(x + v.x, y + v.y)

    override fun minus(v: Vector) : Cartesian = plus(-v)

    override fun unaryMinus(): Cartesian = times(-1.0)

    override fun times(k: Double): Cartesian = Cartesian(x * k, y * k)

    override fun div(k: Double): Cartesian = times(1.0 / k)

    override fun rotate(radians: Double): Cartesian {
        val cos = cos(radians)
        val sin = sin(radians)
        return Cartesian(x * cos - y * sin, x * sin + y * cos)
    }

    override fun normal(): Cartesian {
        if (normal == null) {
            normal = Cartesian(-y, x)
        }
        return normal!!
    }

    override fun module(): Double = hypot(x, y)

    override fun moduleSqr(): Double = pow(module(), 2.0)

    override fun dot(v: Vector): Double {
        val other = v.asCartesian()
        return x * other.x + y * other.y
    }

    override fun cross(v: Vector): Double {
        val other = v.asCartesian()
        return x * other.y - y * other.x
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is Vector) {
            val otherCartesian = other.asCartesian()
            return abs(otherCartesian.x - x) < Vector.PRECISION && abs(otherCartesian.y - y) < Vector.PRECISION
        } else return false
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    override fun toString() : String = "Cartesian(x=$x, y=$y)"

    companion object {
        val ZERO = Cartesian(0.0, 0.0)
    }
}

class Polar(
        /** length of vector, always >= 0  */
        radius: Double,
        /** direction, *radians*, measured from X axis towards Y axis, always within [-PI, +PI]  */
        direction: Double) : Vector {
    val r: Double
    val d: Double
    internal var cartesian: Cartesian? = null
    internal var normal: Polar? = null

    init {
        var d = direction
        if (radius < 0) {
            d += PI
        }
        while (d < -PI) d += 2 * PI
        while (d > PI) d -= 2 * PI
        this.d = d
        this.r = abs(radius)
    }

    override fun asPolar() = this

    override fun asCartesian(): Cartesian {
        if (cartesian == null) {
            cartesian = Cartesian(r * cos(d), r * sin(d))
            cartesian!!.polar = this
        }
        return cartesian!!
    }

    override fun plus(v: Vector) = asCartesian().plus(v.asCartesian())

    override fun minus(v: Vector) = plus(-v)

    override fun unaryMinus(): Polar = times(-1.0)

    override fun times(k: Double): Polar {
        if (k == 0.0) {
            return ZERO
        } else {
            return Polar(r * abs(k), d + if (k > 0) 0.0 else PI)
        }
    }

    override fun div(k: Double) = times(1.0 / k)

    override fun rotate(radians: Double) = Polar(r, d + radians)

    override fun normal(): Polar {
        if (normal == null) {
            normal = Polar(r, d + PI / 2)
        }
        return normal!!
    }

    override fun module() = r

    override fun moduleSqr() = r * r

    override fun dot(v: Vector): Double {
        val other = v.asPolar()
        return r * other.r * cos(d - other.d)
    }

    override fun cross(v: Vector): Double {
        val other = v.asPolar()
        return r * other.r * sin(other.d - d)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        val polar: Polar
        if (other is Vector) {
            polar = other.asPolar()
            return abs(polar.r - r) < Vector.PRECISION && abs(polar.d - d) < Vector.PRECISION
        } else return false
    }

    override fun hashCode(): Int {
        var result = r.hashCode()
        result = 31 * result + d.hashCode()
        return result
    }

    override fun toString(): String = "Polar(r=$r, d=$d)"

    companion object {
        val ZERO = Polar(0.0, 0.0)
    }
}