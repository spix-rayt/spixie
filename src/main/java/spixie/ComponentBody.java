package spixie;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ComponentBody extends ScrollPane {
    final VBox elements = new VBox();
    public ComponentBody() {
        super();
        final BorderPane borderPane = new BorderPane();
        final Button addElementButton = new Button("Add element");
        borderPane.setBottom(addElementButton);
        borderPane.setCenter(elements);
        setContent(borderPane);

        widthProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                addElementButton.setPrefWidth(t1.doubleValue()-2);
            }
        });

        addElementButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                elements.getChildren().addAll(new Multiplier());
            }
        });
    }
}
