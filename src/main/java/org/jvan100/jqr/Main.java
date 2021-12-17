package org.jvan100.jqr;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));

        final Parent root = loader.load();
        final Scene scene = new Scene(root);

        final Controller controller = loader.getController();

        primaryStage.setScene(scene);
        primaryStage.setTitle("JQR");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

}
