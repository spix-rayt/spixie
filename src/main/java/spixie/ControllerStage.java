package spixie;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ControllerStage extends Stage {
    public ControllerStage() {
        super();
        BorderPane borderPane = new BorderPane();
        SplitPane splitPane = new SplitPane();
        final ListView<Component> componentListView = new ListView<>();
        splitPane.setOrientation(Orientation.HORIZONTAL);

        final BorderPane componentBodyContainer = new BorderPane();

        componentListView.getItems().addAll(Main.world.root);
        componentListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Component>() {
            public void changed(ObservableValue<? extends Component> observableValue, Component component, Component t1) {
                componentBodyContainer.setCenter(componentListView.getSelectionModel().getSelectedItem().componentBody);
            }
        });


        componentListView.getSelectionModel().select(0);


        splitPane.getItems().addAll(componentListView, componentBodyContainer);
        splitPane.setDividerPosition(0,0.15);

        borderPane.setCenter(splitPane);


        HBox menuBar = new HBox();
        Button renderButton = new Button("Render");
        renderButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                Main.world.renderToFile();
            }
        });
        menuBar.getChildren().addAll(renderButton,Main.world.frame);
        borderPane.setTop(menuBar);

        Scene scene = new Scene(borderPane, 900, 700);
        scene.getStylesheets().add("style.css");
        setScene(scene);
        setTitle("Spixie");

        setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent windowEvent) {
                Main.world.getCurrentRenderThread().interrupt();
                Platform.exit();
            }
        });
    }
}
