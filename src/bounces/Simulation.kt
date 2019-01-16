package bounces

import org.apache.commons.math3.util.FastMath.*
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JFrame
import javax.swing.JPanel

fun main(args: Array<String>) {
    Simulation().start()
}

class Simulation {

    fun start() {
        EventQueue.invokeLater {
            val activity = Activity()
            activity.isVisible = true
        }
    }
}

private const val timeStep = 10L
private const val maxSize = 100

class Activity : JFrame() {init {
    title = "Bounces"
    size = Dimension(400, 400)
    defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val board = Board()
    add(board)
    addKeyListener(BoardKeysListener(board))

    Timer(true)
            .scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    board.repaint()
                }
            }, timeStep, timeStep)
}
}

class Board : JPanel(true) {
    private val fpsCounter = AtomicInteger()
    private var fpsValue = AtomicInteger()
    private val bodies = mutableListOf<Body>()
    private val topWall = Wall(Cartesian(.0, .0), Cartesian(.0, .0), Cartesian(0.0, 1.0))
    private val leftWall = Wall(Cartesian(.0, .0), Cartesian(.0, .0), Cartesian(1.0, 0.0))
    //    private val rightWall = Wall(Cartesian(.0, .0), Cartesian(.0, .0), Cartesian(-1.0, 0.0))
    private val bottomWall = Wall(Cartesian(.0, .0), Cartesian(.0, .0), Cartesian(0.0, -1.0))
    private val random = Random()
    private val wallColor = Color.red
    private val bodyColor = Color.black

    init {
        Timer(true)
                .scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        fpsValue.set(fpsCounter.getAndSet(0))
                    }
                }, 1000, 1000)
        bodies.addAll(listOf(topWall, leftWall, bottomWall))
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        positionWalls()
        draw(g as Graphics2D)
        fpsCounter.incrementAndGet()
    }

    private fun draw(g: Graphics2D) {
        var line = 1;
        g.drawString("FPS ${fpsValue.get()}", 10,  10 * line++)
        g.drawString("Collisions ${bodies.map { it.collisions }.max()}", 10,  10 * line++)
        val i = Physics.update(bodies, timeStep / 1000.0)
        g.drawString("mX ${reducePrecision(i.momentumX)}", 10, 10 * line++)
        g.drawString("mY ${reducePrecision(i.momentumY)}", 10, 10 * line++)
        g.drawString("mA ${reducePrecision(i.angularMomentum)}", 10, 10 * line++)
        g.drawString("E  ${reducePrecision(i.energy)}", 10, 10 * line++)

        bodies.forEach {
            when (it) {
                is Circle -> draw(it, g)
                is Rect -> draw(it, g)
                is Wall -> draw(it, g)
            }
        }
    }

    private fun reducePrecision(value: Double) = if (abs(value) < Vector.PRECISION) 0.0 else value

    private fun draw(circle: Circle, g: Graphics2D) {
        val x: Int = (circle.center.asCartesian().x - circle.radius).toInt()
        val y: Int = (circle.center.asCartesian().y - circle.radius).toInt()
        val d = circle.radius.toInt() * 2
        g.color = bodyColor
        g.drawOval(x, y, d, d)
        drawLine(g, circle.center, circle.center + circle.turn * circle.radius)
    }

    private fun draw(rect: Rect, g: Graphics2D) {
        g.color = bodyColor

        drawLine(g, rect.p1, rect.p2)
        drawLine(g, rect.p2, rect.p3)
        drawLine(g, rect.p3, rect.p4)
        drawLine(g, rect.p4, rect.p1)
    }

    private fun drawLine(g: Graphics2D, from: Cartesian, to: Cartesian) {
        g.drawLine(from.x.toInt(), from.y.toInt(), to.x.toInt(), to.y.toInt())
    }

    private fun draw(wall: Wall, g: Graphics2D) {
        g.color = wallColor
        g.drawRect(wall.from.x.toInt(), wall.from.y.toInt(), wall.size.x.toInt(), wall.size.y.toInt())
    }

    private fun positionWalls() {
        topWall.size = Cartesian(width.toDouble() - 1, .0)
        leftWall.size = Cartesian(.0, height.toDouble() - 1)
//        rightWall.from = topWall.size
//        rightWall.size = leftWall.size
        bottomWall.from = leftWall.size
        bottomWall.size = topWall.size
    }

    fun addCircle() {
        bodies.add(Circle(randomSize() + 10, Cartesian(width / 2.0, height / 2.0),
                randomSpeed(), Polar(1.0, 0.0), randomAngularSpeed(), 10.0))
    }

    private fun randomSize() = random.nextDouble() * maxSize

    private fun randomSpeed() = Cartesian(randomSize() - maxSize / 2, randomSize() - maxSize / 2) * 2.0

    private fun randomAngularSpeed() = (random.nextDouble() * 2 - 1) * 2

    fun removeBody() {
        val body = bodies.last()
        if (body !is Wall) bodies.remove(body)
    }

    fun piSetup() {
        bodies.clear()
        bodies.addAll(listOf(topWall, leftWall, bottomWall))
        val radius = 40.0
        val center = (topWall.from + bottomWall.from) / 2.0 + Cartesian(600.0, 0.0)
        val lightMass = 1.0
        val heavyMass = lightMass * pow(10.0, 6)
        bodies.add(Circle(radius * 2, center + Cartesian(300.0, 0.0), Polar(30.0, PI), Polar(1.0, .0), 0.0, heavyMass))
        bodies.add(Circle(radius, center, Polar.ZERO, Polar(1.0, 0.0), 0.0, lightMass))
    }

}

class BoardKeysListener(private val b: Board) : KeyListener {
    override fun keyTyped(e: KeyEvent?) {
        when (e?.keyChar) {
            'o' -> b.addCircle()
            '-' -> b.removeBody()
            'p' -> b.piSetup()
        }
    }

    override fun keyPressed(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }
}