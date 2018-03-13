package bounces

import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.Graphics2D
import java.time.Instant
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel

fun main(args: Array<String>) {
    Simulation().start()
}

class Simulation {

    fun start() {
        EventQueue.invokeLater { Activity().isVisible = true }
    }
}

class Activity() : JFrame() {init {
    val board = Board()
    add(board)
    title = "Bounces"
    size = Dimension(400, 400)
    defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    Timer(true)
            .scheduleAtFixedRate(object: TimerTask(){
                override fun run() {

                    board.repaint()
                }
            }, 100, 100)
}
}

class Board : JPanel(true) {
    private val radius = 100
    private var circleY = height / 2 - radius

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        draw(g)
    }

    private fun draw(g: Graphics?) {
        val graphics2D = g as Graphics2D

        graphics2D.drawString("" + Instant.now().epochSecond, 100, 100)
        circleY = (circleY + 5) % (height - radius*2)
        graphics2D.drawOval(width / 2 - radius, circleY, radius * 2, radius * 2)

    }
}
