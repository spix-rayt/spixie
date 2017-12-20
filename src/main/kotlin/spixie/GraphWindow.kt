package spixie

import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ScrollPane
import javafx.scene.control.ToolBar
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import java.util.*
import kotlin.collections.ArrayList

class GraphWindow() : BorderPane(), WorkingWindowOpenableContent {
    private val canvas = Canvas()
    private val g: GraphicsContext
    private var unitWidthInPixels = 50.0
    private var startX = 0.0
    private var startDragX = 0.0
    private var mouseX: Double? = null
    private var mouseY: Double? = null
    private var mousePressPoint: Point? = null
    private val graphData = GraphData()
    private val control = ToolBar()
    private val maxOutputValue = ValueControl(1.0, 1.0, "Max Output")

    val value = Value<Double>(0.5)

    init {
        value.calcNewValue = { inputNewValue ->
            (1.0 - graphData.getValue(inputNewValue)) * maxOutputValue.value.value
        }
        maxOutputValue.set(1.0)
        canvas.onMousePressed = EventHandler<MouseEvent> { mouseEvent ->
            mousePressPoint = Point(mouseEvent.x, mouseEvent.y)
            val mouseValueX = (mouseEvent.x + startX) / unitWidthInPixels
            val mouseValueY = mouseEvent.y / canvas.height
            if (mouseEvent.button == MouseButton.MIDDLE) {
                startDragX = startX + mouseEvent.x.toLong()
            }
            if (mouseEvent.button == MouseButton.PRIMARY) {
                graphData.insertOrUpdatePoint(mouseValueX, mouseValueY, false)
                paint()
            }
        }
        canvas.onMouseDragged = EventHandler<MouseEvent> { mouseEvent ->
            val mouseValueX = (mouseEvent.x + startX) / unitWidthInPixels
            val mouseValueY = mouseEvent.y / canvas.height
            if (mouseEvent.button == MouseButton.MIDDLE) {
                startX = startDragX - mouseEvent.x.toLong()
                if (startX < 0) {
                    startX = 0.0
                }
                paint()
            }
            if (mouseEvent.button == MouseButton.PRIMARY) {
                graphData.insertOrUpdatePoint(mouseValueX, mouseValueY, true)
                paint()
            }

            mouseX = mouseEvent.x
            mouseY = mouseEvent.y
            paint()
        }
        canvas.onMouseMoved = EventHandler<MouseEvent> { mouseEvent ->
            mouseX = mouseEvent.x
            mouseY = mouseEvent.y
            paint()
        }
        canvas.onMouseExited = EventHandler<MouseEvent> {
            mouseX = null
            mouseY = null
            paint()
        }
        canvas.onScroll = EventHandler<ScrollEvent> { scrollEvent ->
            scrollEvent.consume()
            if (scrollEvent.deltaY > 0) {
                if (unitWidthInPixels < 0x10000L) {
                    unitWidthInPixels *= 2.0
                    startX *= 2.0
                }
            }
            if (scrollEvent.deltaY < 0) {
                if (unitWidthInPixels > 0.001) {
                    unitWidthInPixels /= 2.0
                    startX /= 2.0
                }
            }
            paint()
        }
        g = canvas.graphicsContext2D
        val canvasScrollPane = ScrollPane()
        canvasScrollPane.content = canvas
        canvasScrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        center = canvasScrollPane
        canvasScrollPane.widthProperty().addListener { _, _, newValue ->
            canvas.width = newValue.toDouble() - 2
            paint()
        }
        canvasScrollPane.heightProperty().addListener { _, _, newValue ->
            canvas.height = newValue.toDouble() - 2
            paint()
        }
        canvas.width = Main.workingWindow.prefWidth -2
        canvas.height = Main.workingWindow.prefHeight - 2
        paint()

        top = control
        control.items.addAll(maxOutputValue)
    }

    private fun paint() {
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)
        g.lineWidth = 1.0
        paintRule()
        paintLines()
        value.update()
    }

    private var beatStep = 1.0

    private fun paintRule() {
        var beat = 0.0
        var step = unitWidthInPixels
        beatStep = 1.0
        while (step < 50) {
            step *= 2.0
            beatStep *= 2.0
        }
        while (step >= 100) {
            step /= 2.0
            beatStep /= 2.0
        }
        var x = -startX
        var quarter = 0
        while (x < -step) {
            x += step
            quarter++
            beat += beatStep
        }
        while (x < canvas.width) {
            if (quarter % 4 == 0) {
                g.stroke = Color(0.0, 0.0, 0.0, 1.0)
            } else {
                g.stroke = Color(0.0, 0.0, 0.0, 0.4)
            }
            g.strokeLine(x, 0.0, x, canvas.height)
            if (quarter % 4 == 0) {
                g.stroke = Color(0.0, 0.0, 0.0, 0.3)
                g.font = Font(9.0)
                g.strokeText(beat.toString(), x + 2, 9.0, step)
            }
            beat += beatStep
            x += step
            quarter++
        }
    }

    private fun paintLines() {
        g.lineWidth = 1.0

        g.stroke = Color(1.0, 0.0, 0.0, 1.0)
        var x = 0
        while (x < canvas.width) {
            val y1 = graphData.getValue((startX + x) / unitWidthInPixels)
            val y2 = graphData.getValue((startX + x.toDouble() + 1.0) / unitWidthInPixels)
            g.strokeLine(x.toDouble(), y1 * canvas.height, (x + 1).toDouble(), y2 * canvas.height)
            x++
        }
    }

    private fun magnetX(x: Double): Double {
        val fourthBeatStep = beatStep * 0.25
        return Math.round(x / fourthBeatStep) * fourthBeatStep
    }
}
