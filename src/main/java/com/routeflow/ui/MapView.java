package com.routeflow.ui;

import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import com.routeflow.model.Ruta;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Mapa interactivo usando JavaFX WebView + Leaflet.js sobre OpenStreetMap.
 *
 * Toda la comunicación es Java→JS via engine.executeScript().
 * Las llamadas antes de que el mapa cargue se encolan y se ejecutan
 * automáticamente al detectar Worker.State.SUCCEEDED.
 */
public class MapView extends Region {

    private final WebView  webView;
    private final WebEngine engine;
    private boolean mapaListo = false;
    private final List<String> cola = new ArrayList<>();

    public MapView() {
        webView = new WebView();
        engine  = webView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                mapaListo = true;
                Platform.runLater(() -> {
                    cola.forEach(script -> ejecutarAhora(script));
                    cola.clear();
                });
            } else if (state == Worker.State.FAILED) {
                System.err.println("MapView: fallo al cargar el HTML del mapa.");
            }
        });

        URL url = MapView.class.getResource("/com/routeflow/map.html");
        if (url != null) {
            engine.load(url.toExternalForm());
        } else {
            System.err.println("MapView: no se encontró map.html en recursos.");
        }

        getChildren().add(webView);
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());
    }

    // ─── API pública ─────────────────────────────────────────────────────────

    /** Dibuja todos los nodos y aristas del grafo en el mapa. */
    public void init(Grafo grafo) {
        StringBuilder sb = new StringBuilder();
        for (Ciudad c : grafo.getCiudades()) {
            sb.append(String.format(
                "agregarMarcador('%s','%s',%s,%s,'%s');",
                c.getId(), esc(c.getNombre()),
                fmt(c.getLatitud()), fmt(c.getLongitud()),
                c.isEsBodega() ? "bodega" : "cliente"));
        }
        sb.append(String.format("dibujarAristas('%s');", aristasJson(grafo)));
        js(sb.toString());
    }

    /** Ruta directa Dijkstra. */
    public void mostrarRuta(List<String> ids, Grafo grafo, String colorHex) {
        js(String.format("dibujarRuta('%s','%s','Ruta Directa — Dijkstra');",
                caminoJson(ids, grafo), colorHex));
    }

    /** Ruta delivery. */
    public void mostrarRutaDelivery(List<String> ids, Grafo grafo, String colorHex) {
        js(String.format("dibujarRuta('%s','%s','Ruta de Delivery');",
                caminoJson(ids, grafo), colorHex));
    }

    /** Cambia el estilo de la línea de ruta: "solido" o "punteado". */
    public void setEstiloRuta(String estilo) {
        js("setEstiloRuta('" + estilo + "');");
    }

    /** Anima el vehículo a lo largo del camino con el color de carrocería indicado. */
    public void animarVehiculo(List<String> ids, Grafo grafo, String colorHex) {
        js(String.format("animarVehiculo('%s',900,'%s');", caminoJson(ids, grafo), colorHex));
    }

    /** Muestra el panel de información de ruta. */
    public void mostrarInfoRuta(double distKm, double tiempoMin, int hora,
                                 int paradas, List<String> pasos) {
        js(String.format("mostrarInfoRuta(%s,%s,%d,%d,'%s');",
                fmt(distKm), fmt(tiempoMin), hora, paradas, pasosJson(pasos)));
    }

    /** Elimina rutas y vehículo, conserva los marcadores de ciudades. */
    public void limpiarRutas() {
        js("limpiarRuta();");
    }

    // ─── Internos ────────────────────────────────────────────────────────────

    private void js(String script) {
        if (mapaListo) {
            Platform.runLater(() -> ejecutarAhora(script));
        } else {
            cola.add(script);
        }
    }

    private void ejecutarAhora(String script) {
        try { engine.executeScript(script); }
        catch (Exception e) { System.err.println("JS error: " + e.getMessage()); }
    }

    // ─── Construcción de JSON ────────────────────────────────────────────────

    private String aristasJson(Grafo grafo) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Ruta r : grafo.getRutas()) {
            Ciudad a = grafo.getCiudad(r.getOrigen());
            Ciudad b = grafo.getCiudad(r.getDestino());
            if (a == null || b == null) continue;
            if (!first) sb.append(',');
            sb.append(String.format(
                    "{\"lat1\":%s,\"lon1\":%s,\"lat2\":%s,\"lon2\":%s,\"km\":%.2f,\"tipo\":\"%s\"}",
                    fmt(a.getLatitud()), fmt(a.getLongitud()),
                    fmt(b.getLatitud()), fmt(b.getLongitud()),
                    r.getDistanciaKm(), r.getTipoCamino().name()));
            first = false;
        }
        return sb.append(']').toString();
    }

    private String caminoJson(List<String> ids, Grafo grafo) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            Ciudad c = grafo.getCiudad(ids.get(i));
            if (c == null) continue;
            if (i > 0) sb.append(',');
            sb.append(String.format("{\"lat\":%s,\"lon\":%s}",
                    fmt(c.getLatitud()), fmt(c.getLongitud())));
        }
        return sb.append(']').toString();
    }

    private String pasosJson(List<String> pasos) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pasos.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(esc(pasos.get(i))).append('"');
        }
        return sb.append(']').toString();
    }

    /** Formatea double con punto decimal (Locale.US), seguro para JSON/JS. */
    private static String fmt(double v) {
        return String.format(Locale.US, "%.6f", v);
    }

    /** Escapa caracteres conflictivos para insertar en un string JS con comillas simples. */
    private static String esc(String s) {
        return s.replace("\\", "\\\\")
                .replace("'",  "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}