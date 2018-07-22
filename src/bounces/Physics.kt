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
        val movables: List<Movable> = bodies.filter { it is Movable }.map { it as Movable }
        if (movables.isEmpty()) return Invariants(.0, .0, .0, .0)
        return movables
                .map {
                    Invariants(it.speed.asCartesian().x * it.mass,
                            it.speed.asCartesian().y * it.mass,
                            it.speed.moduleSqr() * it.mass / 2 + it.angularSpeed.sqr() * it.inertia / 2,
                            it.angularSpeed * it.inertia)
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
        val collision = findCollision(wallLine, circle)

        if (collision.point != null) applyCollisionResponse(wall, circle, collision.normal, collision.point)
    }

    private fun checkAndCollide(wall: Wall, rect: Rect) {
        val wallLine = StraightLine(wall.from, wall.from + wall.size)
        val collisionPoint = (rect.edges()
                .map { wallLine.findIntersection(it) }
                .filter { it != null } as List<Vector>)
                .average()

        if (collisionPoint != null) applyCollisionResponse(wall, rect, wall.inside, collisionPoint)
    }

    private fun List<Vector>.average(): Vector? {
        return if (this.isEmpty()) null else this.reduce(Vector::plus) / this.size.toDouble()
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
        second.speed += normal * J / second.mass
        second.angularSpeed += rBPn.dot(normal * J) / second.inertia
    }

    private fun checkAndCollide(circle: Circle, rect: Rect) {

    }

    private fun checkAndCollide(first: Rect, second: Rect) {
        val collisionPoint = (first.edges()
                .flatMap { line -> second.edges().map { line.findIntersection(it) } }
                .filter { it != null } as List<Vector>)
                .average()
        if (collisionPoint != null) {
            val firstClosesVertexDistance = first.vertices().map { (collisionPoint - it).module() }.min()!!
            val secondClosesVertexDistance = second.vertices().map { (collisionPoint - it).module() }.min()!!
            val edge = (if (firstClosesVertexDistance >= secondClosesVertexDistance) first else second)
                    .edges().minBy { it.distanceTo(collisionPoint) }!!
            var normal = (edge.to - edge.from).normal()
            if (normal.dot(second.center - first.center) < 0.0) normal *= -1.0

            applyCollisionResponse(first, second, normal, collisionPoint)
        }
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

    private fun findCollision(first: StraightLine, second: Circle): Collision {
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
                if (abs(y1 - y2) < Vector.PRECISION) points = points.take(1)

            }
        } else {
            // (kx + b)**2 + x**2 = r**2
            // (k**2+1) x**2 + 2kb x + b**2 - r**2 = 0
            val k = tan(movedLine.direction)
            val b = movedLine.yFunction(0.0)
            points = squareRoots(k.sqr() + 1, 2 * k * b, b.sqr() - second.radius.sqr())
                    .map { x -> Cartesian(x, movedLine.yFunction(x)) }
        }

        return Collision(points
                .map { it + second.center }
                .filter { first.contains(it) && second.contains(it) }
                .average(),
                orthogonalToLineInDirectionTo(first, second))
    }

    private fun orthogonalToLineInDirectionTo(line: StraightLine, directTo: Circle): Vector {
        val orthogonal = Polar(1.0, line.direction + PI / 2)
        val direction = directTo.center - line.from
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

data class Collision(val point: Vector?,
                     /**
                      * a vector from first to second body, along which a rebound force must be applied.
                      * Can be of any length, since its length will cancel itself during calculations
                      */
                     val normal: Vector)

val emptyCollision = Collision(null, Polar.ZERO)

data class Invariants(val momentumX: Double, val momentumY: Double, val energy: Double, val angularMomentum: Double) {
    operator fun plus(other: Invariants): Invariants {
        return Invariants(momentumX + other.momentumX, momentumY + other.momentumY,
                energy + other.energy, angularMomentum + other.angularMomentum)
    }


}