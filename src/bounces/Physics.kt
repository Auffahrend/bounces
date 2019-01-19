package bounces

import org.apache.commons.math3.util.FastMath.*

object Physics {

    private const val e = 1.0

    fun update(wall: Wall, left: Circle, right: Circle, timeStep: Double): Invariants {
        left.move(timeStep)
        right.move(timeStep)

        checkAndCollide(wall, left)
        checkAndCollide(left, right)

        return Invariants(left) + Invariants(right)
    }

    private fun checkAndCollide(wall: Wall, circle: Circle) {
        val collision = findCollision(wall, circle)

        if (collision.point != null) applyCollisionResponse(wall, circle, collision.normal, collision.point)
    }

    private fun checkAndCollide(first: Circle, second: Circle) {
        val relDistance = (second.center - first.center)
        val penetration = first.radius + second.radius - relDistance.module()
        val collision =
                if (penetration > 0.0) Collision(first.center / 2.0 + second.center / 2.0, relDistance)
                else emptyCollision

        if (collision.point != null) applyCollisionResponse(first, second, collision.normal, collision.point)
    }

    private fun applyCollisionResponse(wall: Wall, second: Movable, normal: Vector, point: Vector) {
        val relSpeed = second.speedAt(point)
        if (relSpeed.dot(wall.inside) > 0) return

        val rBPn = (second.center - point).normal()
        val J = -relSpeed.dot(normal) * (1 + e) / // m**2/s
                ((normal.dot(normal) * (1 / second.mass)) // m**2 / kg
                        + (rBPn.dot(normal)).sqr() / second.inertia)

        second.speed += normal * J / second.mass
        second.angularSpeed += rBPn.dot(normal * J) / second.inertia
        wall.collisions++
        second.collisions++
    }

    private fun applyCollisionResponse(first: Movable, second: Movable, normal: Vector, point: Vector) {
        val relSpeed = second.speedAt(point) - first.speedAt(point)
        if (relSpeed.dot(second.center - first.center) > 0.0) return

        val rAPn = (first.center - point).normal()
        val rBPn = (second.center - point).normal()
        val J = -relSpeed.dot(normal) * (1 + e) / // m**2/s
                ((normal.dot(normal) * (1 / first.mass + 1 / second.mass)) // m**2 / kg
                        + (rAPn.dot(normal)).sqr() / first.inertia
                        + (rBPn.dot(normal)).sqr() / second.inertia)

        first.speed -= normal * J / first.mass
        first.angularSpeed += rAPn.dot(normal * J) / first.inertia
        first.collisions++
        second.speed += normal * J / second.mass
        second.angularSpeed += rBPn.dot(normal * J) / second.inertia
        second.collisions++
    }

    fun linear(line: StraightLine) = linear(line.from.x, line.from.y, line.to.x, line.to.y)

    /**
     * @return linear function by given points
     */
    fun linear(x1: Double, y1: Double, x2: Double, y2: Double): (Double) -> Double {
        if (y2 == y1) {
            return { y2 }
        }

        if (x2 == x1) {
            throw IllegalArgumentException("Can not interpolate by 1 point!")
        }

        // y = kx + y0
        val k = (y2 - y1) / (x2 - x1)
        val y0 = y1 - x1 * k
        return { x -> k * x + y0 }
    }

    private fun findCollision(first: Wall, second: Circle): Collision {
        // moving coordinates center to circle center to simplify
        val x = first.from.x

        if (second.center.x - second.radius <= x) {
            return Collision(Cartesian(x, second.center.y), first.inside)
        } else{
            return Collision(null, first.inside)
        }
    }
}

private fun Double.sqr(): Double = this * this

data class Collision(val point: Vector?,
                     /**
                      * a vector from first to second body, along which a rebound force must be applied.
                      * Can be of any length, since its length will cancel itself during calculations
                      */
                     val normal: Vector)

val emptyCollision = Collision(null, Polar.ZERO)

data class Invariants(val momentumX: Double, val momentumY: Double, val energy: Double, val angularMomentum: Double) {
    constructor(body: Movable) : this(body.speed.asCartesian().x * body.mass,
            body.speed.asCartesian().y * body.mass,
            body.speed.moduleSqr() * body.mass / 2 + body.angularSpeed.sqr() * body.inertia / 2,
            body.angularSpeed * body.inertia)

    operator fun plus(other: Invariants): Invariants {
        return Invariants(momentumX + other.momentumX, momentumY + other.momentumY,
                energy + other.energy, angularMomentum + other.angularMomentum)
    }


}