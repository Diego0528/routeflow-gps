package com.routeflow;

import com.routeflow.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación JavaFX.
 *
 * JavaFX requiere que el hilo principal sea el "Application Thread".
 * launch() se encarga de inicializarlo correctamente.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        MainWindow ventana =






                new MainWindow(stage);
        Scene escena = new Scene(ventana, 1100, 700);

        stage.setTitle("RouteFlow GPS — Antigua Guatemala");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.setScene(escena);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}