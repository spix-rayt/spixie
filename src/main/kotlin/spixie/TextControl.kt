package spixie

import io.reactivex.subjects.PublishSubject
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox

class TextControl(initial: String, val name: String) : HBox() {
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

        labelValue.setOnContextMenuRequested { event ->
            event.consume()
            children.remove(labelValue)
            children.addAll(textFieldValue)
            textFieldValue.text = value
            textFieldValue.requestFocus()
            textFieldValue.selectAll()
        }

        textFieldValue.focusedProperty().addListener { _, _, focused ->
            if (!focused) {
                value = textFieldValue.text

                children.remove(textFieldValue)
                children.addAll(labelValue)
            }
        }

        textFieldValue.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            if(event.code == KeyCode.ESCAPE || event.code == KeyCode.ENTER){
                children.remove(textFieldValue)
                event.consume()
            }
        }

        children.addAll(labelName, labelValue)
        value = initial
    }
}
