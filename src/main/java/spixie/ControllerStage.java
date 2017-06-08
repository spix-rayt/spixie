package spixie;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

public class ControllerStage extends BorderPane {
    public ControllerStage() {
        super();
        SplitPane splitPane = new SplitPane();

        final ListView<Component> componentListView = new ListView<>();
        splitPane.setResizableWithParent(componentListView, false);
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

        setCenter(splitPane);

        ToolBar menuBar = new ToolBar();
        Button renderButton = new Button("Render");
        renderButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                ControllerStage.this.setDisable(true);
                Main.world.renderToFile(new World.FrameRenderedToFileEvent() {
                    @Override
                    public void handle(int currentFrame, int framesCount) {
                        renderButton.setText(currentFrame + " / " + framesCount);
                    }
                }, new World.RenderToFileCompleted() {
                    @Override
                    public void handle() {
                        renderButton.setText("Render");
                        ControllerStage.this.setDisable(false);
                    }
                });
            }
        });
        menuBar.getItems().addAll(renderButton,Main.world.frame);
        setTop(menuBar);
    }
}
