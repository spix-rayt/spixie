package spixie.visualEditor

import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window

class ModuleSettingsDialog(owner: Window, val module: Module): Stage() {
    init {
        val grid = GridPane()
        val nameTextField = TextField(module.name)
        grid.add(Label("Name"), 0, 0)
        grid.add(nameTextField, 1, 0)
        val saveButton = Button("Save")
        val closeButton = Button("Close")
        grid.add(HBox(saveButton, closeButton), 1, 1)

        grid.columnConstraints.addAll(ColumnConstraints(300.0), ColumnConstraints(500.0))
        grid.vgap = 8.0

        saveButton.setOnAction {
            module.name = nameTextField.text
            hide()
        }

        closeButton.setOnAction {
            hide()
        }

        scene = Scene(grid)
        initOwner(owner)
        initModality(Modality.APPLICATION_MODAL)
        showAndWait()
    }
}