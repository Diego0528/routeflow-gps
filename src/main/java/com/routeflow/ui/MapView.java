package com.routeflow.ui;

import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import com.routeflow.model.Ruta;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Collection;
import java.util.List;

/**
 * Mapa interactivo implementado con JavaFX Canvas.
 *
 * Convierte coordenadas GPS (latitud/longitud) a píxeles de pantalla
 * usando una proyección lineal simple. Para el área pequeña de Antigua
 * Guatemala (~25 km²) la distorsión es despreciable.
 *
 * Ventaja sobre WebView+Leaflet: no requiere internet ni módulos JDK
 * específicos, funciona en cualquier versión de Java 21+.
 */
public class MapView extends Pane {

    private final Canvas canvas;
    private final GraphicsContext gc;

    // Límites geográficos del área visible (con margen)
    private static final double MIN_LAT =  14.44;
    private static final double MAX_LAT =  14.66;
    private static final double MIN_LON = -90.83;
    private static final double MAX_LON = -90.62;

    // Estado actual del mapa (para redibujar al cambiar tamaño)
    private Grafo grafoActual;
    private Collection<Ciudad> ciudadesActuales;
    private List<String> rutaActual;
    private Color colorRuta = Color.web("#2980b9");

    private Timeline animacion;

    public MapView() {
        canvas = new Canvas();
        getChildren().add(canvas);
        gc = canvas.getGraphicsContext2D();

        // El canvas se adapta al tamaño del contenedor padre
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // Redibujar siempre que cambie el tamaño de la ventana
        widthProperty().addListener((obs, o, n) -> redibujar());
        heightProperty().addListener((obs, o, n) -> redibujar());

        setStyle("-fx-background-color: #1a2634;");
        dibujarFondo();
    }

    // ─── Conversión de coordenadas ─────────────────────────────────────────────

    private double lonToX(double lon) {
        return (lon - MIN_LON) / (MAX_LON - MIN_LON) * canvas.getWidth();
    }

    private double latToY(double lat) {
        // Invertido: latitudes mayores = más arriba en pantalla
        return (1.0 - (lat - MIN_LAT) / (MAX_LAT - MIN_LAT)) * canvas.getHeight();
    }

    // ─── Métodos de dibujo internos ───────────────────────────────────────────

    private void redibujar() {
        if (canvas.getWidth() == 0 || canvas.getHeight() == 0) return;
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        dibujarFondo();
        if (grafoActual != null) dibujarConexiones(grafoActual);
        if (rutaActual != null && grafoActual != null) dibujarRutaColoreada(rutaActual, colorRuta);
        if (ciudadesActuales != null) dibujarCiudades(ciudadesActuales);
    }

    private void dibujarFondo() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#1a2634"));
        gc.fillRect(0, 0, w, h);

        // Cuadrícula de referencia
        gc.setStroke(Color.web("#1e3050", 0.8));
        gc.setLineWidth(0.5);
        int lineas = 10;
        for (int i = 0; i <= lineas; i++) {
            double x = w * i / lineas;
            double y = h * i / lineas;
            gc.strokeLine(x, 0, x, h);
            gc.strokeLine(0, y, w, y);
        }

        // Borde del área
        gc.setStroke(Color.web("#2c4a6e"));
        gc.setLineWidth(1);
        gc.strokeRect(1, 1, w - 2, h - 2);

        // Leyenda en la esquina inferior izquierda
        gc.setFill(Color.web("#e74c3c"));
        gc.fillOval(12, h - 30, 10, 10);
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("Bodega", 26, h - 21);

        gc.setFill(Color.web("#3498db"));
        gc.fillOval(90, h - 30, 10, 10);
        gc.fillText("Ciudad", 104, h - 21);

        gc.setFill(Color.web("#2ecc71"));
        gc.fillOval(160, h - 30, 10, 10);
        gc.fillText("Vehículo", 174, h - 21);
    }

    private void dibujarConexiones(Grafo grafo) {
        gc.setStroke(Color.web("#2c4a6e"));
        gc.setLineWidth(1.5);
        gc.setLineDashes(4, 4);
        for (Ruta r : grafo.getRutas()) {
            Ciudad a = grafo.getCiudad(r.getOrigen());
            Ciudad b = grafo.getCiudad(r.getDestino());
            if (a != null && b != null) {
                gc.strokeLine(lonToX(a.getLongitud()), latToY(a.getLatitud()),
                              lonToX(b.getLongitud()), latToY(b.getLatitud()));
            }
        }
        gc.setLineDashes(null); // restaurar línea sólida
    }

    private void dibujarRutaColoreada(List<String> ids, Color color) {
        if (ids.size() < 2 || grafoActual == null) return;
        gc.setStroke(color);
        gc.setLineWidth(4);
        for (int i = 0; i < ids.size() - 1; i++) {
            Ciudad a = grafoActual.getCiudad(ids.get(i));
            Ciudad b = grafoActual.getCiudad(ids.get(i + 1));
            if (a != null && b != null) {
                gc.strokeLine(lonToX(a.getLongitud()), latToY(a.getLatitud()),
                              lonToX(b.getLongitud()), latToY(b.getLatitud()));
            }
        }
    }

    private void dibujarCiudades(Collection<Ciudad> ciudades) {
        for (Ciudad c : ciudades) {
            double x = lonToX(c.getLongitud());
            double y = latToY(c.getLatitud());
            double r = c.isEsBodega() ? 10 : 7;
            Color color = c.isEsBodega() ? Color.web("#e74c3c") : Color.web("#3498db");

            // Halo de sombra
            gc.setFill(Color.color(0, 0, 0, 0.25));
            gc.fillOval(x - r + 2, y - r + 2, r * 2, r * 2);

            // Círculo del nodo
            gc.setFill(color);
            gc.fillOval(x - r, y - r, r * 2, r * 2);

            // Borde blanco
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(x - r, y - r, r * 2, r * 2);

            // Etiqueta del nombre
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            String nombre = c.getNombre();
            // Abreviar nombres muy largos para que quepan en el mapa
            if (nombre.length() > 20) nombre = nombre.substring(0, 18) + "…";
            gc.fillText(nombre, x + r + 3, y + 4);
        }
    }

    private void dibujarVehiculo(double x, double y) {
        double r = 9;
        // Halo pulsante (círculo exterior semitransparente)
        gc.setFill(Color.web("#2ecc71", 0.25));
        gc.fillOval(x - r * 2, y - r * 2, r * 4, r * 4);
        // Círculo verde sólido
        gc.setFill(Color.web("#2ecc71"));
        gc.fillOval(x - r, y - r, r * 2, r * 2);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(x - r, y - r, r * 2, r * 2);
    }

    // ─── API pública ──────────────────────────────────────────────────────────

    /** Dibuja todos los nodos del grafo como marcadores en el mapa. */
    public void mostrarCiudades(Collection<Ciudad> ciudades) {
        ciudadesActuales = ciudades;
        redibujar();
    }

    /** Dibuja la ruta calculada con una línea azul. */
    public void mostrarRuta(List<String> caminoIds, Grafo grafo) {
        grafoActual = grafo;
        rutaActual = caminoIds;
        colorRuta = Color.web("#2980b9");
        redibujar();
    }

    /** Dibuja la ruta de delivery con una línea naranja. */
    public void mostrarRutaDelivery(List<String> caminoIds, Grafo grafo) {
        grafoActual = grafo;
        rutaActual = caminoIds;
        colorRuta = Color.web("#e67e22");
        redibujar();
    }

    /** Elimina la ruta dibujada pero mantiene los marcadores de ciudades. */
    public void limpiarRutas() {
        rutaActual = null;
        if (animacion != null) animacion.stop();
        redibujar();
    }

    /**
     * Anima un marcador verde que recorre el camino nodo a nodo.
     * Usa JavaFX Timeline con 1 segundo de intervalo entre nodos.
     */
    public void animarRecorrido(List<String> caminoIds, Grafo grafo) {
        if (animacion != null) animacion.stop();
        if (caminoIds.isEmpty()) return;

        animacion = new Timeline();
        animacion.setCycleCount(1);

        for (int i = 0; i < caminoIds.size(); i++) {
            final int idx = i;
            KeyFrame kf = new KeyFrame(Duration.seconds(i), ev -> {
                Ciudad c = grafo.getCiudad(caminoIds.get(idx));
                if (c == null) return;
                // Redibujar el mapa base y encima el vehículo
                redibujar();
                dibujarVehiculo(lonToX(c.getLongitud()), latToY(c.getLatitud()));
            });
            animacion.getKeyFrames().add(kf);
        }
        animacion.play();
    }
}