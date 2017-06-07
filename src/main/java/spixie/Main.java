package spixie;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    public static World world;
    public void start(Stage stage) throws Exception {
        world = new World();
        StackPane root = new StackPane();
        final ImageView imageView = new ImageView();
        imageView.setSmooth(true);
        imageView.setPreserveRatio(false);
        root.getChildren().addAll(imageView);

        Scene scene = new Scene(root);
        imageView.fitWidthProperty().bind(root.widthProperty());
        imageView.fitHeightProperty().bind(root.heightProperty());
        stage.setTitle("Render");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        world.renderStart(imageView);

        new ControllerStage().show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
