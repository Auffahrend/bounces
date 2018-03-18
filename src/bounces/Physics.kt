package bounces

import org.apache.commons.math3.util.FastMath

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
        val xDiameter = StraightLine(circle.center.asCartesian() + Cartesian(-circle.radius, .0),
                circle.center.asCartesian() + Cartesian(circle.radius, .0))
        val yDiameter = StraightLine(circle.center.asCartesian() + Cartesian(.0, -circle.radius),
                circle.center.asCartesian() + Cartesian(.0, circle.radius))
        if (!findIntersection(wallLine, xDiameter).isEmpty()) {
            val currentSpeed = circle.speed.asCartesian()
            circle.speed = Cartesian(-currentSpeed.x, currentSpeed.y)
        }
        if (!findIntersection(wallLine, yDiameter).isEmpty()) {
            val currentSpeed = circle.speed.asCartesian()
            circle.speed = Cartesian(currentSpeed.x, -currentSpeed.y)
        }
    }

    private fun checkAndCollide(wall: Wall, rect: Rect) {
        val wallLine = StraightLine(wall.from, wall.from + wall.size)
        if (wallLine.isVertical) {
        }
    }

    private fun checkAndCollide(first: Circle, second: Circle) {
        var relative = (second.center - first.center)
        val relSpeed = second.speed - first.speed
        val penetration = first.radius + second.radius - relative.module()
        if (penetration > 0.0 && relative.dot(relSpeed) < 0) {
            val newFirstSpeed = (first.speed * (first.mass - second.mass) + second.speed * (2 * second.mass)) / (first.mass + second.mass)
            second.speed = (second.speed * (second.mass - first.mass) + first.speed * (2 * first.mass)) / (first.mass + second.mass)
            first.speed = newFirstSpeed
        }
    }

    private fun checkAndCollide(circle: Circle, rect: Rect) {

    }

    private fun checkAndCollide(first: Rect, second: Rect) {

    }

    private fun findIntersection(first: StraightLine, second: StraightLine): List<Intersection<StraightLine, StraightLine>> {
        return if (isSecondLineCrossesFirst(first, second) && isSecondLineCrossesFirst(second, first)) {
            listOf(Intersection(first, second, getStraightsIntersectionPoint(first, second)))
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

}

data class Intersection<out L1 : Line, out L2 : Line>(val line1: L1, val line2: L2, val point: Vector)

data class Invariants(val momentumX: Double, val momentumY: Double, val energy: Double) {
    operator fun plus(other: Invariants): Invariants {
        return Invariants(momentumX + other.momentumX, momentumY + other.momentumY, energy + other.energy)
    }
}