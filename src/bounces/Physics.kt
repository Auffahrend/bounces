package bounces

import org.apache.commons.math3.util.FastMath
import org.apache.commons.math3.util.FastMath.*

object Physics {

    private const val e = 1.0

    fun update(bodies: List<Body>, timeStep: Double): Invariants {
        bodies.filter { it is Movable }.forEach { (it as Movable).move(timeStep) }

        collide(bodies)

        return invariants(bodies)
    }

    private fun invariants(bodies: List<Body>): Invariants {
        val movable : List<Movable> = bodies.filter { it is Movable }.map { it as Movable }
        if (movable.isEmpty()) return Invariants(.0, .0, .0)
        return movable
                .map {
                    Invariants(it.speed.asCartesian().x * it.mass,
                            it.speed.asCartesian().y * it.mass,
                            it.speed.moduleSqr() * it.mass / 2)
                }
                .reduce { i1, i2 -> i1 + i2 }
    }

    private fun collide(bodies: List<Body>) {
        for (i in 0 until bodies.size - 1)
            for (j in i + 1 until bodies.size)
                checkAndCollide(bodies[i], bodies[j])
    }

    private fun checkAndCollide(first: Body, second: Body) {
        when (first) {
            is Wall -> checkAndCollide(first, second)
            is Circle -> checkAndCollide(first, second)
            is Rect -> checkAndCollide(first, second)
        }
    }

    private fun checkAndCollide(wall: Wall, body: Body) {
        when (body) {
            is Circle -> checkAndCollide(wall, body)
            is Rect -> checkAndCollide(wall, body)
        }
    }

    private fun checkAndCollide(circle: Circle, body: Body) {
        when (body) {
            is Circle -> checkAndCollide(body, circle)
            is Rect -> checkAndCollide(circle, body)
        }
    }

    private fun checkAndCollide(rect: Rect, body: Body) {
        when (body) {
            is Circle -> checkAndCollide(body, rect)
            is Rect -> checkAndCollide(rect, body)
        }
    }

    private fun checkAndCollide(wall: Wall, circle: Circle) {
        val wallLine = StraightLine(wall.from, wall.from + wall.size)
        val intersections = findIntersections(wallLine, circle)
        if (!intersections.isEmpty() && circle.speed.dot(wall.inside) < 0) {
            val currentSpeed = circle.speed.asCartesian()
            if (wallLine.isVertical) {
                circle.speed = Cartesian(-currentSpeed.x, currentSpeed.y)
            } else {
                circle.speed = Cartesian(currentSpeed.x, -currentSpeed.y)
            }
        }
    }

    private fun checkAndCollide(wall: Wall, rect: Rect) {
        val wallLine = StraightLine(wall.from, wall.from + wall.size)
        if (!rect.lines().flatMap { findIntersections(wallLine, it) }.isEmpty()
                && rect.speed.dot(wall.inside) < 0) {
            val currentSpeed = rect.speed.asCartesian()
            if (wallLine.isVertical) {
                rect.speed = Cartesian(-currentSpeed.x, currentSpeed.y)
            } else {
                rect.speed = Cartesian(currentSpeed.x, -currentSpeed.y)
            }
        }
    }

    private fun checkAndCollide(first: Circle, second: Circle) {
        val relDistance = (second.center - first.center)
        val relSpeed = second.speed - first.speed
        val penetration = first.radius + second.radius - relDistance.module()
        if (penetration > 0.0 && relDistance.dot(relSpeed) < 0) {
            applyCollisionResponse(relSpeed, first, second)
        }
    }

    private fun applyCollisionResponse(relSpeed: Vector, first: Movable, second: Movable) {
        var relDistance = (second.center - first.center)
        relDistance /= relDistance.module()
        val J = -relSpeed.dot(relDistance) * (1 + e) / (relDistance.dot(relDistance) * (1 / first.mass + 1 / second.mass))

        first.speed -= relDistance * J / first.mass
        second.speed += relDistance * J / second.mass
    }

    private fun checkAndCollide(circle: Circle, rect: Rect) {

    }

    private fun checkAndCollide(first: Rect, second: Rect) {

    }

    private fun findIntersections(first: StraightLine, second: StraightLine): List<Vector> {
        return if (isSecondLineCrossesFirst(first, second) && isSecondLineCrossesFirst(second, first)) {
            listOf(getStraightsIntersectionPoint(first, second))
                    .filter { first.contains(it) && second.contains(it) }
        } else {
            emptyList()
        }
    }

    private fun isSecondLineCrossesFirst(first: StraightLine, second: StraightLine): Boolean {
        val product1 = (first.to - first.from).cross(second.from - first.from)
        val product2 = (first.to - first.from).cross(second.to - first.from)
        return product1 * product2 < 0
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

    private fun getStraightsIntersectionPoint(first: StraightLine, second: StraightLine): Vector {
        return when {
            first.isVertical -> Cartesian(first.from.x, second.yFunction(first.from.x))
            second.isVertical -> Cartesian(second.from.x, first.yFunction(second.from.x))
            else -> {
                val x = (second.yFunction(0.0) - first.yFunction(0.0)) / (FastMath.tan(first.direction) - FastMath.tan(second.direction))
                Cartesian(x, first.yFunction(x))
            }
        }
    }

    private fun findIntersections(first: StraightLine, second: Circle): List<Intersection> {
        // moving coordinates center to circle center to simplify
        val movedLine = StraightLine(first.from - second.center, first.to - second.center)
        var points = emptyList<Vector>()
        if (first.isVertical) {
            if (movedLine.from.x in -abs(second.radius)..abs(second.radius)) {
                val y1 = second.innerY1Function(movedLine.from.x)
                val y2 = second.innerY2Function(movedLine.from.x)
                points = listOf(
                        Cartesian(movedLine.from.x, y1),
                        Cartesian(movedLine.from.x, y2))
                if (abs(y1-y2) < Vector.PRECISION) points = points.take(1)

            }
        } else {
            // (kx + b)**2 + x**2 = r**2
            // (k**2+1) x**2 + 2kb x + b**2 - r**2 = 0
            val k = tan(movedLine.direction)
            val b = movedLine.yFunction(0.0)
            points = squareRoots(k.sqr() + 1, 2 * k * b, b.sqr() - second.radius.sqr())
                    .map { x -> Cartesian(x, movedLine.yFunction(x)) }
        }

        return points
                .map { it + second.center }
                .filter { first.contains(it) && second.contains(it) }
                .sortedWith ( compareBy({ it.asCartesian().x }, { it.asCartesian().y }) )
                .map { Intersection(it, orthogonalToLineInDirectionTo(first, second, it)) }
    }

    private fun orthogonalToLineInDirectionTo(line: StraightLine, directTo: Circle, point: Vector): Vector {
        val orthogonal = Polar(1.0, line.direction + PI / 2)
        val direction = directTo.center - point
        return if (orthogonal.dot(direction) > Vector.PRECISION) orthogonal else -orthogonal
    }

    private fun squareRoots(a: Double, b: Double, c: Double): List<Double> {
        val d = b.sqr() - 4 * a * c
        if (d >= 0) {
            val x1 = (sqrt(d) - b) / (2 * a)
            val x2 = (-sqrt(d) - b) / (2 * a)

            return if (d < Vector.PRECISION) {
                listOf(x1)
            } else {
                listOf(x1, x2)
            }
        }
        return emptyList()
    }

}

private fun Double.sqr(): Double = this * this

data class Intersection(val point: Vector,
                        /**
                         * a vector from first to second body, along which a rebound force must be applied
                         */
                        val rebounde: Vector)

data class Invariants(val momentumX: Double, val momentumY: Double, val energy: Double) {
    operator fun plus(other: Invariants): Invariants {
        return Invariants(momentumX + other.momentumX, momentumY + other.momentumY, energy + other.energy)
    }


}