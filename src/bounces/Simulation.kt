package bounces

import org.apache.commons.math3.util.FastMath.*
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
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
    private val leftWall = Wall(Cartesian(.0, .0), Cartesian(.0, 1000.0), Cartesian(1.0, 0.0))
    private val rect1 = Circle(20.0, Cartesian(200.0, 500.0), Cartesian(.0, .0), Polar(1.0, .0), .0, 1.0)
    private val rect2 = Circle(50.0, Cartesian(500.0, 500.0), Cartesian(.0, .0), Polar(1.0, .0), .0, 1000000000000.0)
    private val wallColor = Color.red
    private val bodyColor = Color.black

    private val invariants = AtomicReference(Invariants(0.0, 0.0, 0.0, 0.0))

    init {
        piSetup()
        Timer(true)
                .scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        fpsValue.set(fpsCounter.getAndSet(0))
                    }
                }, 1000, 1000)
        val physicsThread = Thread {
            while (true) {
                invariants.set(Physics.update(leftWall, rect1, rect2, 1E-7))
            }
        }
        physicsThread.isDaemon = true
        physicsThread.start()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        positionWalls()
        draw(g as Graphics2D)
        fpsCounter.incrementAndGet()
    }

    private fun draw(g: Graphics2D) {
        var line = 1;
        g.drawString("FPS ${fpsValue.get()}", 10, 10 * line++)
        g.drawString("Collisions ${rect1.collisions}", 10, 10 * line++)
        g.drawString("mX ${reducePrecision(invariants.get().momentumX)}", 10, 10 * line++)
        g.drawString("mY ${reducePrecision(invariants.get().momentumY)}", 10, 10 * line++)
        g.drawString("mA ${reducePrecision(invariants.get().angularMomentum)}", 10, 10 * line++)
        g.drawString("E  ${reducePrecision(invariants.get().energy)}", 10, 10 * line++)

        draw(leftWall, g)
        draw(rect1, g)
        draw(rect2, g)
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

    private fun drawLine(g: Graphics2D, from: Cartesian, to: Cartesian) {
        g.drawLine(from.x.toInt(), from.y.toInt(), to.x.toInt(), to.y.toInt())
    }

    private fun draw(wall: Wall, g: Graphics2D) {
        g.color = wallColor
        g.drawRect(wall.from.x.toInt(), wall.from.y.toInt(), wall.size.x.toInt(), wall.size.y.toInt())
    }

    private fun positionWalls() {
        leftWall.size = Cartesian(.0, height.toDouble() - 1)
    }

    fun piSetup() {
        val lightMass = 1.0
        val heavyMass = lightMass * pow(10.0, 6)
        rect1.center = Cartesian(200.0, 200.0)
        rect2.center = Cartesian(400.0, 200.0)
        rect1.speed = Cartesian(0.0, 0.0)
        rect2.speed = Cartesian(-10.0, 0.0)
        rect1.collisions=0
        rect2.collisions=0
        leftWall.collisions=0
    }
}

class BoardKeysListener(private val b: Board) : KeyListener {
    override fun keyTyped(e: KeyEvent?) {
        when (e?.keyChar) {
            'p' -> b.piSetup()
        }
    }

    override fun keyPressed(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }
}