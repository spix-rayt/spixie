package spixie;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    public static World world;
    public void start(Stage stage) throws Exception {
        world = new World();
        BorderPane root = new BorderPane();
        final ImageView imageView = new ImageView();

        root.setCenter(imageView);

        Scene scene = new Scene(root, 800, 600);
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                imageView.setFitWidth(t1.doubleValue());
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                imageView.setFitHeight(t1.doubleValue());
            }
        });
        stage.setTitle("Render");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        world.render(imageView);



        new ControllerStage().show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
