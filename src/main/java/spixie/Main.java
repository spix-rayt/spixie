package spixie;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jfxtras.scene.control.window.Window;

public class Main extends Application {
    public static World world;
    public void start(Stage stage) throws Exception {
        world = new World();
        Group root = new Group();
        final ImageView imageView = new ImageView();
        imageView.setSmooth(true);
        imageView.setPreserveRatio(false);
        root.getChildren().addAll(imageView);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        imageView.fitWidthProperty().bind(scene.widthProperty());
        imageView.fitHeightProperty().bind(scene.heightProperty());
        stage.setTitle("Render");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        world.renderStart(imageView);

        ControllerStage controllerStage = new ControllerStage();

        Window window = new Window("");
        window.getContentPane().getChildren().addAll(controllerStage);
        window.setPrefSize(800,600);
        window.setMinSize(400,300);
        controllerStage.prefWidthProperty().bind(window.getContentPane().widthProperty());
        controllerStage.prefHeightProperty().bind(window.getContentPane().heightProperty());
        window.setResizableWindow(true);
        window.setMovable(true);
        window.setResizableBorderWidth(4);
        root.getChildren().addAll(window);

        final Float[] windowOpacity = {1.0f};


        root.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.DIGIT1 && event.isControlDown()) windowOpacity[0] = 1.0f;
                if(event.getCode() == KeyCode.DIGIT2 && event.isControlDown()) windowOpacity[0] = 0.8f;
                if(event.getCode() == KeyCode.DIGIT3 && event.isControlDown()) windowOpacity[0] = 0.6f;
                if(event.getCode() == KeyCode.DIGIT4 && event.isControlDown()) windowOpacity[0] = 0.4f;
                if(event.getCode() == KeyCode.DIGIT5 && event.isControlDown()) windowOpacity[0] = 0.2f;
                if(event.getCode() == KeyCode.TAB){
                    window.setStyle("-fx-opacity: 0.0");
                }else{
                    window.setStyle("-fx-opacity: " + windowOpacity[0]);
                }
            }
        });

        root.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.TAB){
                    window.setStyle("-fx-opacity: " + windowOpacity[0]);
                }
            }
        });

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent windowEvent) {
                Main.world.getCurrentRenderThread().interrupt();
                Platform.exit();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
