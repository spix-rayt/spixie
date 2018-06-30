package spixie

import io.reactivex.subjects.PublishSubject
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox

class TextControl(initial: String, private val name: String) : HBox() {
    private val labelName = Label()
    private val labelValue = Label()
    private val textFieldValue = TextField()

    var value = ""
        set(value) {
            field = value
            labelValue.text = field
            changes.onNext(field)
        }
    val changes = PublishSubject.create<String>()

    override fun toString(): String {
        return "TextControl($name $value)"
    }

    init {
        if(name.isNotEmpty()){
            labelName.text = "$name: "
        }else{
            labelName.text = ""
        }

        labelValue.styleClass.add("label-value")

        labelValue.onMouseReleased = EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mouseEvent.consume()
                children.remove(labelValue)
                children.addAll(textFieldValue)
                textFieldValue.text = value
                textFieldValue.requestFocus()
                textFieldValue.selectAll()
            }
        }

        textFieldValue.focusedProperty().addListener { _, _, t1 ->
            if (!t1) {
                try {
                    value = textFieldValue.text
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
    }
}
