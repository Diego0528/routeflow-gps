package com.routeflow.ui;

import com.routeflow.algorithm.Dijkstra;
import com.routeflow.algorithm.FloydWarshall;
import com.routeflow.data.DataManager;
import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ventana principal de la aplicación.
 *
 * Organiza la interfaz con BorderPane: controles a la izquierda, mapa al centro.
 * Toda la lógica de negocio (algoritmos) vive en las clases de algorithm/,
 * esta clase solo conecta la UI con esa lógica.
 */
public class MainWindow extends BorderPane {

    private Grafo grafo;
    private MapView mapView;
    private Label barraEstado;

    private ComboBox<String> comboOrigen;
    private ComboBox<String> comboDestino;
    private ComboBox<String> comboCandidato;
    private ListView<String> listaEntregas;
    private ObservableList<String> ciudadesEntrega = FXCollections.observableArrayList();

    // Mapea "Nombre (ID)" → id real de la ciudad, para los ComboBox
    private java.util.Map<String, String> etiquetaAId = new java.util.HashMap<>();

    public MainWindow(Stage stage) {
        construirUI();
        cargarDatosIniciales();
    }

    // ─── Construcción de la UI ────────────────────────────────────────────────

    private void construirUI() {
        setTop(crearBannerSuperior());
        setLeft(crearPanelControles());
        setCenter(crearPanelMapa());
        setBottom(crearBarraEstado());
    }

    private HBox crearBannerSuperior() {
        HBox banner = new HBox();
        banner.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 12;");

        Label titulo = new Label("RouteFlow GPS");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.WHITE);

        Label subtitulo = new Label("  |  Simulador de delivery — Antigua Guatemala");
        subtitulo.setFont(Font.font("Arial", 14));
        subtitulo.setTextFill(Color.LIGHTGRAY);

        banner.getChildren().addAll(titulo, subtitulo);
        return banner;
    }

    private VBox crearPanelControles() {
        VBox panel = new VBox(8);
        panel.setPrefWidth(220);
        panel.setPadding(new Insets(12));
        panel.setStyle("-fx-background-color: #16213e;");

        // ── Sección Ruta Directa ──────────────────────────────────────────
        Label lblRuta = estilo(new Label("RUTA DIRECTA"), true);

        comboOrigen = new ComboBox<>();
        comboOrigen.setPromptText("Ciudad origen");
        comboOrigen.setMaxWidth(Double.MAX_VALUE);

        comboDestino = new ComboBox<>();
        comboDestino.setPromptText("Ciudad destino");
        comboDestino.setMaxWidth(Double.MAX_VALUE);

        Button btnDijkstra = boton("Calcular Ruta Directa", "#2980b9");
        btnDijkstra.setOnAction(e -> calcularRutaDirecta());

        // ── Separador ────────────────────────────────────────────────────
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #2c3e50;");

        // ── Sección Delivery ─────────────────────────────────────────────
        Label lblDelivery = estilo(new Label("RUTA DELIVERY"), true);
        Label lblAgregar = estilo(new Label("Selecciona ciudad y presiona '+':"), false);

        comboCandidato = new ComboBox<>();
        comboCandidato.setPromptText("Ciudad a entregar");
        comboCandidato.setMaxWidth(Double.MAX_VALUE);

        Button btnAgregarEntrega = boton("+ Agregar a ruta", "#27ae60");
        btnAgregarEntrega.setOnAction(e -> {
            String seleccion = comboCandidato.getValue();
            if (seleccion != null && !ciudadesEntrega.contains(seleccion)) {
                ciudadesEntrega.add(seleccion);
            }
        });

        listaEntregas = new ListView<>(ciudadesEntrega);
        listaEntregas.setPrefHeight(120);
        listaEntregas.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white;");

        Button btnLimpiarEntregas = boton("Limpiar lista", "#7f8c8d");
        btnLimpiarEntregas.setOnAction(e -> ciudadesEntrega.clear());

        Button btnFloyd = boton("Optimizar Ruta Delivery", "#e67e22");
        btnFloyd.setOnAction(e -> calcularRutaDelivery());

        // ── Separador ────────────────────────────────────────────────────
        Separator sep2 = new Separator();

        // ── Sección Edición ──────────────────────────────────────────────
        Label lblEditar = estilo(new Label("EDITAR MAPA"), true);

        Button btnAgregarCiudad = boton("+ Agregar Ciudad", "#8e44ad");
        btnAgregarCiudad.setOnAction(e -> mostrarDialogoCiudad());

        Button btnAgregarRuta = boton("+ Agregar Ruta", "#8e44ad");
        btnAgregarRuta.setOnAction(e -> mostrarDialogoRuta());

        Button btnGuardar = boton("Guardar Mapa", "#16a085");
        btnGuardar.setOnAction(e -> guardarMapa());

        panel.getChildren().addAll(
            lblRuta, comboOrigen, comboDestino, btnDijkstra,
            sep1,
            lblDelivery, lblAgregar, comboCandidato, btnAgregarEntrega,
            listaEntregas, btnLimpiarEntregas, btnFloyd,
            sep2,
            lblEditar, btnAgregarCiudad, btnAgregarRuta, btnGuardar
        );

        return panel;
    }

    private StackPane crearPanelMapa() {
        mapView = new MapView();
        StackPane contenedor = new StackPane(mapView);
        return contenedor;
    }

    private HBox crearBarraEstado() {
        HBox barra = new HBox();
        barra.setStyle("-fx-background-color: #0f3460; -fx-padding: 6 12;");
        barraEstado = new Label("Iniciando RouteFlow GPS...");
        barraEstado.setTextFill(Color.LIGHTGRAY);
        barra.getChildren().add(barraEstado);
        return barra;
    }

    // ─── Carga inicial de datos ───────────────────────────────────────────────

    private void cargarDatosIniciales() {
        try {
            String rutaArchivo = "data/antigua_map.txt";
            if (!new File(rutaArchivo).exists()) {
                // Fallback: buscar relativo al directorio donde se ejecuta
                rutaArchivo = System.getProperty("user.dir") + "/data/antigua_map.txt";
            }
            grafo = DataManager.cargarDesdeArchivo(rutaArchivo);
            poblarComboBox();
            // El Canvas está listo de inmediato — no hay que esperar carga asíncrona
            mapView.mostrarCiudades(grafo.getCiudades());
            setEstado("Mapa cargado: " + grafo.getNodosCount()
                    + " ciudades, " + grafo.getAristasCount() + " rutas");

        } catch (Exception e) {
            setEstado("Error al cargar el mapa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void poblarComboBox() {
        etiquetaAId.clear();
        List<String> etiquetas = new ArrayList<>();

        for (Ciudad c : grafo.getCiudades()) {
            String etiqueta = c.getNombre() + " (" + c.getId() + ")";
            etiquetas.add(etiqueta);
            etiquetaAId.put(etiqueta, c.getId());
        }

        etiquetas.sort(String::compareTo);
        ObservableList<String> items = FXCollections.observableArrayList(etiquetas);
        comboOrigen.setItems(items);
        comboDestino.setItems(items);

        comboCandidato.setItems(items);
    }

    // ─── Acciones de la UI ────────────────────────────────────────────────────

    private void calcularRutaDirecta() {
        String etOrigen = comboOrigen.getValue();
        String etDestino = comboDestino.getValue();
        if (etOrigen == null || etDestino == null) {
            setEstado("Selecciona origen y destino.");
            return;
        }
        String origenId = etiquetaAId.get(etOrigen);
        String destinoId = etiquetaAId.get(etDestino);

        if (origenId.equals(destinoId)) {
            setEstado("Origen y destino son la misma ciudad.");
            return;
        }

        Dijkstra.ResultadoDijkstra resultado = Dijkstra.calcular(grafo, origenId, destinoId);

        mapView.limpiarRutas();
        if (!resultado.encontrado) {
            setEstado("No existe camino entre las ciudades seleccionadas.");
            return;
        }

        mapView.mostrarRuta(resultado.camino, grafo);
        mapView.animarRecorrido(resultado.camino, grafo);

        String pasos = String.join("\n", resultado.pasosDescripcion);
        setEstado(String.format("Ruta directa: %.2f km | Pasos: %d | %s → %s",
                resultado.distanciaTotal,
                resultado.camino.size() - 1,
                grafo.getCiudad(origenId).getNombre(),
                grafo.getCiudad(destinoId).getNombre()));

        System.out.println("=== Ruta Dijkstra ===\n" + pasos);
    }

    private void calcularRutaDelivery() {
        if (ciudadesEntrega.isEmpty()) {
            setEstado("Agrega al menos una ciudad de entrega.");
            return;
        }

        // Buscar la bodega como punto de partida
        String bodegaId = grafo.getCiudades().stream()
                .filter(Ciudad::isEsBodega)
                .map(Ciudad::getId)
                .findFirst()
                .orElse(null);

        if (bodegaId == null) {
            setEstado("No hay bodega definida en el mapa.");
            return;
        }

        FloydWarshall.MatrizDistancias matriz = FloydWarshall.calcular(grafo);

        // Heurística greedy: siempre ir al destino más cercano que queda pendiente
        List<String> pendientes = ciudadesEntrega.stream()
                .map(etiquetaAId::get)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(ArrayList::new));

        List<String> rutaCompleta = new ArrayList<>();
        rutaCompleta.add(bodegaId);
        String posicionActual = bodegaId;

        while (!pendientes.isEmpty()) {
            String siguiente = matriz.getCiudadMasCercana(posicionActual, pendientes);
            pendientes.remove(siguiente);

            // Calcular camino real entre posición actual y siguiente con Dijkstra
            Dijkstra.ResultadoDijkstra tramo = Dijkstra.calcular(grafo, posicionActual, siguiente);
            if (tramo.encontrado && tramo.camino.size() > 1) {
                // Evitar duplicar el nodo de conexión entre tramos
                rutaCompleta.addAll(tramo.camino.subList(1, tramo.camino.size()));
            }
            posicionActual = siguiente;
        }

        mapView.limpiarRutas();
        mapView.mostrarRutaDelivery(rutaCompleta, grafo);
        mapView.animarRecorrido(rutaCompleta, grafo);

        double distanciaTotal = calcularDistanciaRuta(rutaCompleta);
        setEstado(String.format("Delivery optimizado: %.2f km | %d entregas | Ruta: %s",
                distanciaTotal, ciudadesEntrega.size(),
                rutaCompleta.stream()
                    .map(id -> grafo.getCiudad(id) != null ? grafo.getCiudad(id).getNombre() : id)
                    .collect(Collectors.joining(" → "))));
    }

    private double calcularDistanciaRuta(List<String> camino) {
        double total = 0;
        for (int i = 0; i < camino.size() - 1; i++) {
            Ciudad a = grafo.getCiudad(camino.get(i));
            Ciudad b = grafo.getCiudad(camino.get(i + 1));
            if (a != null && b != null) {
                total += com.routeflow.algorithm.Haversine.calcularDistanciaKm(
                        a.getLatitud(), a.getLongitud(),
                        b.getLatitud(), b.getLongitud());
            }
        }
        return total;
    }

    private void mostrarDialogoCiudad() {
        Dialog<Ciudad> dialog = new Dialog<>();
        dialog.setTitle("Agregar Ciudad");
        dialog.setHeaderText("Nueva ciudad al mapa");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField tfId    = new TextField(); tfId.setPromptText("GT011");
        TextField tfNom   = new TextField(); tfNom.setPromptText("Nombre del lugar");
        TextField tfLat   = new TextField(); tfLat.setPromptText("14.5586");
        TextField tfLon   = new TextField(); tfLon.setPromptText("-90.7295");
        CheckBox  cbBodega = new CheckBox("Es bodega");

        grid.add(new Label("ID:"),       0, 0); grid.add(tfId,    1, 0);
        grid.add(new Label("Nombre:"),   0, 1); grid.add(tfNom,   1, 1);
        grid.add(new Label("Latitud:"),  0, 2); grid.add(tfLat,   1, 2);
        grid.add(new Label("Longitud:"), 0, 3); grid.add(tfLon,   1, 3);
        grid.add(cbBodega, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    return new Ciudad(
                        tfId.getText().trim(), tfNom.getText().trim(),
                        Double.parseDouble(tfLat.getText()),
                        Double.parseDouble(tfLon.getText()),
                        cbBodega.isSelected());
                } catch (Exception e) {
                    setEstado("Error: verifica los campos (latitud y longitud deben ser números).");
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            grafo.agregarCiudad(c);
            mapView.mostrarCiudades(List.of(c));
            poblarComboBox();
            setEstado("Ciudad agregada: " + c.getNombre());
        });
    }

    private void mostrarDialogoRuta() {
        List<String> etiquetas = new ArrayList<>(etiquetaAId.keySet());
        etiquetas.sort(String::compareTo);

        Dialog<boolean[]> dialog = new Dialog<>();
        dialog.setTitle("Agregar Ruta");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        ComboBox<String> cbOrigen  = new ComboBox<>(FXCollections.observableArrayList(etiquetas));
        ComboBox<String> cbDestino = new ComboBox<>(FXCollections.observableArrayList(etiquetas));
        CheckBox cbBidi = new CheckBox("Bidireccional");
        cbBidi.setSelected(true);

        grid.add(new Label("Origen:"),  0, 0); grid.add(cbOrigen,  1, 0);
        grid.add(new Label("Destino:"), 0, 1); grid.add(cbDestino, 1, 1);
        grid.add(cbBidi, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> bt == ButtonType.OK
            ? new boolean[]{ cbBidi.isSelected() } : null);

        dialog.showAndWait().ifPresent(flags -> {
            String origenId  = etiquetaAId.get(cbOrigen.getValue());
            String destinoId = etiquetaAId.get(cbDestino.getValue());
            if (origenId == null || destinoId == null) return;
            try {
                grafo.agregarRuta(origenId, destinoId, flags[0]);
                setEstado("Ruta agregada: " + cbOrigen.getValue() + " → " + cbDestino.getValue());
            } catch (Exception e) {
                setEstado("Error al agregar ruta: " + e.getMessage());
            }
        });
    }

    private void guardarMapa() {
        try {
            DataManager.guardarEnArchivo(grafo, "data/antigua_map.txt");
            setEstado("Mapa guardado en data/antigua_map.txt");
        } catch (Exception e) {
            setEstado("Error al guardar: " + e.getMessage());
        }
    }

    // ─── Utilidades de UI ────────────────────────────────────────────────────

    private void setEstado(String mensaje) {
        barraEstado.setText(mensaje);
    }

    private Button boton(String texto, String colorHex) {
        Button b = new Button(texto);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; " +
                   "-fx-font-size: 12px; -fx-cursor: hand;");
        return b;
    }

    private Label estilo(Label label, boolean negrita) {
        label.setTextFill(Color.LIGHTGRAY);
        if (negrita) {
            label.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            label.setPadding(new Insets(6, 0, 2, 0));
        }
        return label;
    }
}