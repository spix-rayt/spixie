package spixie

import io.reactivex.subjects.PublishSubject
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.effect.DropShadow
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import java.awt.Robot
import kotlin.math.floor
import kotlin.math.log

class NumberControl(initial: Double, val name: String, initialScale: Double = 0.0) : HBox() {
    private var mousePressedScreenX = 0.0

    private var mousePressedScreenY = 0.0

    private val labelName = Label()

    private val labelValue = Label()

    private val textFieldValue = TextField()

    var value = 0.0
        set(value) {
            field = value.coerceIn(min, max)
            labelValue.text = String.format("%f", field.toFloat())
            changes.onNext(field)
            redrawNumberLineCanvas()
        }

    var numberLineScale = 0.0
        set(value) {
            field = value.coerceIn(-700.0, 500.0)
            valueMultiplier = Math.pow(10.0, field/100.0)
            redrawNumberLineCanvas()
        }

    var valueMultiplier = 1.0

    private val numberLineCanvas = Canvas().apply {
        width = 500.0
        height = 75.0
        graphicsContext2D.font = Font.font(10.0)

        clip = Rectangle(width, height).apply {
            arcWidth = 75.0
            arcHeight = 75.0
        }
    }

    private fun redrawNumberLineCanvas() {
        val w = numberLineCanvas.width
        val h = numberLineCanvas.height
        numberLineCanvas.graphicsContext2D.apply {
            clearRect(0.0, 0.0, w, h)
            strokeLine(w/2.0 + 0.5, 0.0, w/2.0 + 0.5, h)
            val log = log(valueMultiplier * w * 1.1, 10.0)
            val step = Math.pow(10.0, floor(log))
            val valueRounded = if(value > 0) {
                value - (value % step)
            } else {
                value - (value % step) - step
            }

            val opacity = (1.0 - (log - floor(log).toInt())).coerceIn(0.0, 1.0)
            val blackWithOpacity = Color.rgb(0, 0, 0, opacity)
            for(i in -60..60) {
                val lineValue = valueRounded + step / 10.0 * i
                val x = 250.5 + (lineValue - value) / valueMultiplier
                if(i % 10 == 0) {
                    strokeLine(x, h-50.0, x, h)
                    strokeText(lineValue.toString(), x, h-55.0)
                } else {
                    stroke = blackWithOpacity
                    strokeLine(x, h-45.0, x, h + 5.0)
                    stroke = Color.BLACK
                }
            }
        }
    }

    private val numberLineWindow = Pane().apply {
        setMinSize(500.0, 75.0)
        setMaxSize(500.0, 75.0)
        background = Background(BackgroundFill(Color.WHITE, CornerRadii(75.0 / 2.0), Insets.EMPTY))
        effect = DropShadow()
        children.addAll(numberLineCanvas)
    }

    private val numberLineWindowContainer = StackPane().apply {
        children.addAll(numberLineWindow)
        StackPane.setAlignment(numberLineWindow, Pos.TOP_CENTER)
    }

    val changes = PublishSubject.create<Double>()

    var min = Double.NEGATIVE_INFINITY
        private set

    fun limitMin(min: Double): NumberControl{
        this.min = min
        return this
    }

    var max = Double.POSITIVE_INFINITY
        private set

    fun limitMax(max:Double): NumberControl{
        this.max = max
        return this
    }

    override fun toString(): String {
        return "NumberControl($name $value)"
    }

    init {
        if(name.isNotEmpty()){
            labelName.text = "$name: "
        }else{
            labelName.text = ""
        }

        labelValue.styleClass.add("label-value")

        labelValue.onMousePressed = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mousePressedScreenX = mouseEvent.screenX
                mousePressedScreenY = mouseEvent.screenY
                mouseEvent.consume()
                (this.scene.root as Pane).children.add(numberLineWindowContainer)
            }
            if(mouseEvent.button == MouseButton.SECONDARY) {
                mouseEvent.consume()
            }
        }

        labelValue.onContextMenuRequested = EventHandler { mouseEvent->
            mouseEvent.consume()
        }

        labelValue.onMouseDragged = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mouseEvent.consume()
                if(mouseEvent.screenX != mousePressedScreenX || mouseEvent.screenY != mousePressedScreenY) {
                    Robot().mouseMove(mousePressedScreenX.toInt(), mousePressedScreenY.toInt())
                }
                numberLineScale += mousePressedScreenY - mouseEvent.screenY
                val delta = mouseEvent.screenX - mousePressedScreenX

                value = value+(delta*valueMultiplier)
                labelValue.cursor = Cursor.NONE
            }
        }

        labelValue.onMouseReleased = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                (this.scene.root as Pane).children.remove(numberLineWindowContainer)
                labelValue.cursor = Cursor.H_RESIZE
            }
            if(mouseEvent.button == MouseButton.SECONDARY) {
                if(numberLineWindowContainer.parent == null) {
                    mouseEvent.consume()
                    children.remove(labelValue)
                    children.addAll(textFieldValue)
                    textFieldValue.text = String.format("%f", value)
                    textFieldValue.requestFocus()
                    textFieldValue.selectAll()
                }
            }
        }

        textFieldValue.focusedProperty().addListener { _, _, t1 ->
            if (!t1) {
                try {
                    value = java.lang.Double.parseDouble(textFieldValue.text)
                } catch (e: NumberFormatException) {
                }

                children.remove(textFieldValue)
                children.addAll(labelValue)
            }
        }

        textFieldValue.onKeyPressed = EventHandler<KeyEvent> { keyEvent ->
            if(keyEvent.code == KeyCode.ESCAPE || keyEvent.code == KeyCode.ENTER){
                children.remove(textFieldValue)
                keyEvent.consume()
            }
        }
        children.addAll(labelName, labelValue)
        value = initial

        numberLineScale = initialScale
    }
}
