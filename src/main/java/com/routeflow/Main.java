package com.routeflow;

import com.routeflow.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        MainWindow ventana =






                new MainWindow(stage);
        Scene escena = new Scene(ventana, 1200, 760);

        URL css = Main.class.getResource("/com/routeflow/style.css");
        if (css != null) escena.getStylesheets().add(css.toExternalForm());

        stage.setTitle("RouteFlow GPS — Antigua Guatemala");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(escena);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}