package bounces

import org.apache.commons.math3.util.FastMath.PI
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
    private val topWall = Wall(Cartesian(.0, .0), Cartesian(.0, .0))
    private val leftWall = Wall(Cartesian(.0, .0), Cartesian(.0, .0))
    private val rightWall = Wall(Cartesian(.0, .0), Cartesian(.0, .0))
    private val bottomWall = Wall(Cartesian(.0, .0), Cartesian(.0, .0))
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
        bodies.addAll(listOf(topWall, leftWall, rightWall, bottomWall))
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        positionWalls()
        draw(g as Graphics2D)
        fpsCounter.incrementAndGet()
    }

    private fun draw(g: Graphics2D) {
        bodies.forEach {
            when (it) {
                is Circle -> draw(it, g)
                is Rect -> draw(it, g)
                is Wall -> draw(it, g)
            }
        }

        g.drawString("FPS ${fpsValue.get()}", 10, 10)
        val i = Physics.update(bodies, 0.1)
        g.drawString("X ${i.momentumX}", 10, 20)
        g.drawString("Y ${i.momentumY}", 10, 30)
        g.drawString("E ${i.energy}", 10, 40)

    }

    private fun draw(circle: Circle, g: Graphics2D) {
        val x: Int = (circle.center.asCartesian().x - circle.radius).toInt()
        val y: Int = (circle.center.asCartesian().y - circle.radius).toInt()
        val d = circle.radius.toInt() * 2
        g.color = bodyColor
        g.drawOval(x, y, d, d)
    }

    private fun draw(rect: Rect, g: Graphics2D) {
        g.color = bodyColor

        val halfWidth = rect.turn * (rect.size.x / 2)
        val halfHeight = rect.turn.normal() * (rect.size.y / 2)
        val p1 = (rect.center + halfHeight + halfWidth).asCartesian()
        val p2 = (rect.center - halfHeight + halfWidth).asCartesian()
        val p3 = (rect.center - halfHeight - halfWidth).asCartesian()
        val p4 = (rect.center + halfHeight - halfWidth).asCartesian()
        drawLine(g, p1, p2)
        drawLine(g, p2, p3)
        drawLine(g, p3, p4)
        drawLine(g, p4, p1)
    }

    private fun drawLine(g: Graphics2D, p1: Cartesian, p2: Cartesian) {
        g.drawLine(p1.x.toInt(), p1.y.toInt(), p2.x.toInt(), p2.y.toInt())
    }

    private fun draw(wall: Wall, g: Graphics2D) {
        g.color = wallColor
        g.drawRect(wall.from.x.toInt(), wall.from.y.toInt(), wall.size.x.toInt(), wall.size.y.toInt())
    }

    private fun positionWalls() {
        topWall.size = Cartesian(width.toDouble() - 1, .0)
        leftWall.size = Cartesian(.0, height.toDouble() - 1)
        rightWall.from = topWall.size
        rightWall.size = leftWall.size
        bottomWall.from = leftWall.size
        bottomWall.size = topWall.size
    }

    fun addCircle() {
        bodies.add(Circle(5.0 + randomSize(), Cartesian(width / 2.0, height / 2.0),
                randomSpeed(), Polar(1.0, 0.0), 0.0))
    }

    private fun randomSize() = random.nextDouble() * maxSize

    private fun randomSpeed() = Cartesian(randomSize() - maxSize / 2, randomSize() - maxSize / 2)

    fun removeBody() {
        val body = bodies.last()
        if (body !is Wall) bodies.remove(body)
    }

    fun addRect() {
        bodies.add(Rect(Cartesian(random.nextDouble() * width - maxSize, random.nextDouble() * height - maxSize),
                Cartesian(maxSize / 2 + randomSize() / 2, maxSize / 2 + randomSize() / 2), randomSpeed(),
                Polar(1.0, random.nextDouble() * PI), 0.0))
    }
}

class BoardKeysListener(private val b: Board) : KeyListener {
    override fun keyTyped(e: KeyEvent?) {
        when (e?.keyChar) {
            'o' -> b.addCircle()
            '-' -> b.removeBody()
            'r' -> b.addRect()
        }
    }

    override fun keyPressed(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }
}