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
import javafx.util.Pair
import java.util.*
import kotlin.collections.HashMap

class Graph(val outputValue: Value) : BorderPane(), ValueChanger {
    private val canvas = Canvas()
    private val g: GraphicsContext
    private var unitWidthInPixels = 50.0
    private var startX = 0.0
    private var startDragX = 0.0

    private var mouseX: Double? = null
    private var mouseY: Double? = null
    private var mousePointGrub: Point? = null
    private var mousePointGrubCreatedNow = false
    private var mousePressPoint: Point? = null
    private var gravityPoint: Point? = null


    private val points = ArrayList<Point>()


    private val control = ToolBar()
    private var _inputValue: Value = Value.EMPTY

    var inputValue: Value
    get() = _inputValue
    set(value) {
        _inputValue.item().unsubscribeChanger(this)
        _inputValue = value
        _inputValue.item().subscribeChanger(this)
    }
    private val maxOutputValue = Value(1.0, 1.0, "Max Output", false)

    init {
        height = 200.0
        canvas.height = 200.0
        if(outputValue.get()<1.0){
            maxOutputValue.set(2.0)
        }else{
            maxOutputValue.set(Math.ceil(outputValue.get() * 2))
        }
        points.add(Point(0.0, 0.5))
        sortPoints()
        canvas.onMousePressed = EventHandler<MouseEvent> { mouseEvent ->
            mousePressPoint = Point(mouseEvent.x, mouseEvent.y)
            val mouseValueX = (mouseEvent.x + startX) / unitWidthInPixels
            val mouseValueY = mouseEvent.y / canvas.height
            if (mouseEvent.button == MouseButton.SECONDARY) {
                startDragX = startX + mouseEvent.x.toLong()
            }
            if (mouseEvent.isControlDown) {
                if (mouseEvent.isControlDown) {
                    if (mouseEvent.button == MouseButton.PRIMARY) {
                        for (point in points) {
                            if (point.next != null && point.next!!.x > mouseValueX) {
                                gravityPoint = point
                                break
                            }
                        }
                    }
                }
            } else {
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    val nearestPoint = getNearestPoint(mouseValueX, mouseValueY)

                    if (nearestPoint != null) {
                        mousePointGrub = nearestPoint
                        mousePointGrubCreatedNow = false
                    }


                    if (nearestPoint == null) {
                        val magnetValueX = magnetX(mouseValueX)
                        val value = getValue(magnetValueX)
                        if (Math.abs(value - mouseValueY) < 0.03) {
                            val newPoint = Point(magnetValueX, value)
                            points.add(newPoint)
                            mousePointGrub = newPoint
                            mousePointGrubCreatedNow = true
                            sortPoints()
                            if (newPoint.prev != null && newPoint.next != null) {
                                val gravityPointByX = findGravityPointByX(newPoint.prev!!, newPoint.next!!, magnetValueX)
                                val oldGravityX = newPoint.prev!!.x + (newPoint.next!!.x - newPoint.prev!!.x) * newPoint.prev!!.gravity_x
                                val oldGravityY = newPoint.prev!!.y + (newPoint.next!!.y - newPoint.prev!!.y) * newPoint.prev!!.gravity_y
                                var gravityPoint = findGravityPoint(newPoint.prev!!, Point(oldGravityX, oldGravityY), newPoint.next!!, newPoint.prev!!, newPoint, gravityPointByX.value, false)
                                newPoint.prev!!.gravity_x = (gravityPoint!!.x - newPoint.prev!!.x) / (newPoint.x - newPoint.prev!!.x)
                                newPoint.prev!!.gravity_y = (gravityPoint.y - newPoint.prev!!.y) / (newPoint.y - newPoint.prev!!.y)


                                gravityPoint = findGravityPoint(newPoint.prev!!, Point(oldGravityX, oldGravityY), newPoint.next!!, newPoint, newPoint.next!!, gravityPointByX.value, true)
                                newPoint.gravity_x = (gravityPoint!!.x - newPoint.x) / (newPoint.next!!.x - newPoint.x)
                                newPoint.gravity_y = (gravityPoint.y - newPoint.y) / (newPoint.next!!.y - newPoint.y)
                            }
                        }
                    }
                    paint()
                }
            }
        }
        canvas.onMouseReleased = EventHandler<MouseEvent> { mouseEvent ->
            if (mousePointGrub != null) {
                if (points.size > 1) {
                    if (!mousePointGrubCreatedNow) {
                        if (Math.hypot(mouseEvent.x - mousePressPoint!!.x, mouseEvent.y - mousePressPoint!!.y) < 5) {
                            points.remove(mousePointGrub!!)
                            sortPoints()
                        }
                    }
                }
                mousePointGrub = null
            }
            if (gravityPoint != null) {
                gravityPoint = null
            }
            paint()
        }
        canvas.onMouseDragged = EventHandler<MouseEvent> { mouseEvent ->
            val mouseValueX = (mouseEvent.x + startX) / unitWidthInPixels
            val mouseValueY = mouseEvent.y / canvas.height
            if (mouseEvent.button == MouseButton.SECONDARY) {
                startX = startDragX - mouseEvent.x.toLong()
                if (startX < 0) {
                    startX = 0.0
                }
                paint()
            }
            if (gravityPoint != null) {
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    var mouseLimitedValueX = mouseValueX
                    val minx = Math.min(gravityPoint!!.x, gravityPoint!!.next!!.x)
                    val maxx = Math.max(gravityPoint!!.x, gravityPoint!!.next!!.x)
                    if (mouseLimitedValueX < minx) mouseLimitedValueX = minx
                    if (mouseLimitedValueX > maxx) mouseLimitedValueX = maxx
                    var mouseLimitedValueY = mouseValueY
                    val miny = Math.min(gravityPoint!!.y, gravityPoint!!.next!!.y)
                    val maxy = Math.max(gravityPoint!!.y, gravityPoint!!.next!!.y)
                    if (mouseLimitedValueY < miny) mouseLimitedValueY = miny
                    if (mouseLimitedValueY > maxy) mouseLimitedValueY = maxy
                    gravityPoint!!.gravity_x = (mouseLimitedValueX - gravityPoint!!.x) / (gravityPoint!!.next!!.x - gravityPoint!!.x)
                    gravityPoint!!.gravity_y = (mouseLimitedValueY - gravityPoint!!.y) / (gravityPoint!!.next!!.y - gravityPoint!!.y)
                }
            } else {
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mousePointGrub != null) {
                        val magnetValueX = magnetX(mouseValueX)
                        if (magnetValueX < 0) {
                            mousePointGrub!!.x = 0.0
                        } else {
                            mousePointGrub!!.x = magnetValueX
                        }

                        if (mouseValueY < 0) {
                            mousePointGrub!!.y = 0.0
                        } else if (mouseValueY > 1.0) {
                            mousePointGrub!!.y = 1.0
                        } else {
                            mousePointGrub!!.y = mouseValueY
                        }
                        sortPoints()
                    }
                    paint()
                }
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
        Main.workingWindow.prefWidthProperty().addListener { _, _, newValue ->
            canvas.width = newValue.toDouble() - 2
            paint()
        }
        canvas.width = Main.workingWindow.prefWidth -2
        paint()

        top = control
        maxOutputValue.item().subscribeChanger(object : ValueChanger {
            override fun updateOutValue() {
                if (maxOutputValue.get() < 1) {
                    maxOutputValue.set(1.0)
                }
                paint()
            }
            override val valueToBeChanged = Value.EMPTY.item()
        })
        control.items.addAll(maxOutputValue)
    }

    private fun sortPoints() {
        points.sortWith(Comparator<Point> { p1, p2 -> java.lang.Double.compare(p1.x, p2.x) })
        var last: Point? = null
        for (point in points) {
            point.prev = last
            if (last != null) {
                last.next = point
            }
            last = point
        }
        last!!.next = null
    }

    override fun updateOutValue() {
        if(_inputValue != Value.EMPTY){
            outputValue.set((1.0 - getValue(_inputValue.get())) * maxOutputValue.get())
        }
    }

    override val valueToBeChanged = outputValue.item()

    private fun paint() {
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)
        g.lineWidth = 1.0
        paintRule()
        paintLines()
        updateOutValue()
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

    private fun getNearestPoint(x: Double, y: Double): Point? {
        var nearestDist = java.lang.Double.MAX_VALUE
        var nearestPoint: Point? = null
        for (point in points) {
            val dist = Math.hypot((point.x - x) * unitWidthInPixels / 100, point.y - y)
            if (nearestPoint == null) {
                nearestPoint = point
                nearestDist = dist
            } else {
                if (dist < nearestDist) {
                    nearestPoint = point
                    nearestDist = dist
                }
            }
        }
        if (nearestDist < 0.04) {
            return nearestPoint
        } else {
            return null
        }
    }

    private fun paintLines() {
        val nearestPoint: Point?
        g.lineWidth = 1.0
        var mouseValueX: Double? = null
        var mouseValueY: Double?
        if (mouseX != null && mouseY != null) {
            mouseValueX = (mouseX!! + startX) / unitWidthInPixels
            mouseValueY = mouseY!! / canvas.height
            nearestPoint = getNearestPoint(mouseValueX, mouseValueY)
            if (nearestPoint == null && mousePointGrub == null && gravityPoint == null) {
                if (Math.abs(getValue(mouseValueX) - mouseValueY) < 0.03) {
                    g.lineWidth = 2.2
                }
            }
        } else {
            nearestPoint = null
        }

        g.stroke = Color(1.0, 0.0, 0.0, 1.0)
        var x = 0
        while (x < canvas.width) {
            val y1 = getValue((startX + x) / unitWidthInPixels)
            val y2 = getValue((startX + x.toDouble() + 1.0) / unitWidthInPixels)
            g.strokeLine(x.toDouble(), y1 * canvas.height, (x + 1).toDouble(), y2 * canvas.height)
            x++
        }
        var gravityPointPainted = gravityPoint != null
        for (point in points) {
            g.fill = Color.BLACK
            if (point === nearestPoint) {
                g.fillOval(point.x * unitWidthInPixels - 4.0 - startX, point.y * canvas.height - 4, 8.0, 8.0)
            } else {
                g.fillOval(point.x * unitWidthInPixels - 3.0 - startX, point.y * canvas.height - 3, 6.0, 6.0)
            }
            if (mouseValueX != null && !gravityPointPainted) {
                if (point.next != null && point.next!!.x >= mouseValueX) {
                    g.fill = Color.MEDIUMPURPLE.brighter()
                    val gravityX = point.x + (point.next!!.x - point.x) * point.gravity_x
                    val gravityY = point.y + (point.next!!.y - point.y) * point.gravity_y
                    g.fillOval(gravityX * unitWidthInPixels - 3.0 - startX, gravityY * canvas.height - 3, 6.0, 6.0)
                    gravityPointPainted = true
                }
            }
        }
        if (gravityPoint != null) {
            g.fill = Color.MEDIUMPURPLE.brighter()
            val gravityX = gravityPoint!!.x + (gravityPoint!!.next!!.x - gravityPoint!!.x) * gravityPoint!!.gravity_x
            val gravityY = gravityPoint!!.y + (gravityPoint!!.next!!.y - gravityPoint!!.y) * gravityPoint!!.gravity_y
            g.fillOval(gravityX * unitWidthInPixels - 3.0 - startX, gravityY * canvas.height - 3, 6.0, 6.0)
        }
    }

    private fun getValue(param: Double): Double {
        var p1: Point? = null
        var p3: Point? = null
        if (points.size == 1) {
            return points[0].y
        } else {
            if (param < points[0].x) {
                return points[0].y
            }
            if (param > points[points.size - 1].x) {
                return points[points.size - 1].y
            }
            for (point in points) {
                if (p1 == null) {
                    p1 = point
                } else {
                    if (p3 != null) {
                        p1 = p3
                    }
                    p3 = point
                    if (p3.x >= param) {
                        val gravityPointByX = findGravityPointByX(p1, p3, param)
                        return gravityPointByX.key.y
                    }
                }
            }
            return p3!!.y
        }
    }

    private fun findGravityPointByX(p1: Point, p3: Point, x: Double): Pair<Point, Double> {
        var min_t = 0.0
        var max_t = 1.0
        while (true) {
            val t = (min_t + max_t) / 2
            val megaT = Math.pow(t, 1.0)
            val p2_x = p1.x + (p3.x - p1.x) * p1.gravity_x
            val p2_y = p1.y + (p3.y - p1.y) * p1.gravity_y
            val p1p2x = p1.x + (p2_x - p1.x) * megaT
            val p1p2y = p1.y + (p2_y - p1.y) * megaT
            val p2p3x = p2_x + (p3.x - p2_x) * megaT
            val p2p3y = p2_y + (p3.y - p2_y) * megaT
            val bx = p1p2x + (p2p3x - p1p2x) * t
            val by = p1p2y + (p2p3y - p1p2y) * t
            if (Math.abs(bx - x) < 0.00001) {
                return Pair(Point(bx, by), t)
            }
            if (x > bx) {
                min_t = (max_t + min_t) / 2
            }
            if (x < bx) {
                max_t = (max_t + min_t) / 2
            }
        }
    }

    private fun calcCoords(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, t: Double): Point {
        val l0x = x0 + (x1 - x0) * t
        val l1x = x1 + (x2 - x1) * t
        val x = l0x + (l1x - l0x) * t

        val l0y = y0 + (y1 - y0) * t
        val l1y = y1 + (y2 - y1) * t
        val y = l0y + (l1y - l0y) * t
        return Point(x, y)
    }

    private fun findGravityPoint(p0: Point, p1: Point, p2: Point, p3: Point, p5: Point, k: Double, secondPart: Boolean): Point? {
        var dist = java.lang.Double.MAX_VALUE
        var result: Point? = null
        val xstart: Double
        val xend: Double
        val ystart: Double
        val yend: Double
        if (p5.x < p3.x) {
            xstart = p5.x
            xend = p3.x
        } else {
            xstart = p3.x
            xend = p5.x
        }
        if (p5.y < p3.y) {
            ystart = p5.y
            yend = p3.y
        } else {
            ystart = p3.y
            yend = p5.y
        }
        val xstep = (xend - xstart) / 1000
        val ystep = (yend - ystart) / 1000
        var x = xstart
        while (x < xend) {
            var y = ystart
            while (y < yend) {
                val c0: Point
                if (secondPart) {
                    c0 = calcCoords(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, (k + 1.0) / 2)
                } else {
                    c0 = calcCoords(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, 0.5 * k)
                }

                val c1 = calcCoords(p3.x, p3.y, x, y, p5.x, p5.y, 0.5)
                val d = (c1.x - c0.x) * (c1.x - c0.x) + (c1.y - c0.y) * (c1.y - c0.y)
                if (d < dist) {
                    dist = d
                    result = Point(x, y)
                }
                y += ystep
            }
            x += xstep
        }
        return result
    }

    private fun magnetX(x: Double): Double {
        val fourthBeatStep = beatStep * 0.25
        return Math.round(x / fourthBeatStep) * fourthBeatStep
    }

    companion object{
        val graphMap = HashMap<Value, Graph>()
    }
}
