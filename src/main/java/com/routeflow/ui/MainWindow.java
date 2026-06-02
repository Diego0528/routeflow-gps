package com.routeflow.ui;

import com.routeflow.algorithm.Dijkstra;
import com.routeflow.algorithm.FloydWarshall;
import com.routeflow.algorithm.Haversine;
import com.routeflow.data.DataManager;
import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import com.routeflow.model.Ruta;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindow extends BorderPane {

    // ─── Estado ──────────────────────────────────────────────────────────────
    private Grafo   grafo;
    private MapView mapView;
    private Label   barraEstado;
    private Stage   stage;
    private VBox    panelIzquierdo;

    // ─── Preferencias visuales ────────────────────────────────────────────────
    private String colorCarroDirecto  = "#2563eb";
    private String colorCarroDelivery = "#e67e22";
    private String estiloRutaActual   = "solido";

    // Mapeos etiqueta → id / [origenId, destinoId]
    private final Map<String, String>   etiquetaAId   = new HashMap<>();
    private final Map<String, String[]> etiquetaARuta = new HashMap<>();

    // ─── Controles con estado ─────────────────────────────────────────────────
    private ComboBox<String>       comboOrigen, comboDestino;
    private ComboBox<String>       comboCandidato;
    private ComboBox<String>       comboRuta;
    private Spinner<Integer>       spinnerHora;
    private ListView<String>       listaEntregas;
    private ObservableList<String> ciudadesEntrega = FXCollections.observableArrayList();
    private Slider                 sliderFactor;
    private ToggleButton           btnBloquear;
    private Label                  lblEstadoRuta;
    private Label                  lblResultadoRuta;
    private Label                  lblResultadoDelivery;

    // ─── Estilos comunes ──────────────────────────────────────────────────────
    private static final String BG_PANEL  = "-fx-background-color:#161b22;";
    private static final String BG_MAIN   = "-fx-background-color:#0d1117;";
    private static final String BG_HEADER = "-fx-background-color:#161b22;";
    private static final String BG_STATUS = "-fx-background-color:#161b22; -fx-border-color:#21262d; -fx-border-width:1 0 0 0;";
    private static final String FG_WHITE  = "-fx-text-fill:#e6edf3;";
    private static final String FG_GRAY   = "-fx-text-fill:#7d8590;";
    private static final String FG_BLUE   = "-fx-text-fill:#58a6ff;";
    private static final String FG_GREEN  = "-fx-text-fill:#3fb950;";
    private static final String FG_ORANGE = "-fx-text-fill:#d29922;";
    private static final String FG_RED    = "-fx-text-fill:#f85149;";

    // ─── Constructor ──────────────────────────────────────────────────────────
    public MainWindow(Stage stage) {
        this.stage = stage;
        setStyle(BG_MAIN);
        construirUI();
        cargarDatosIniciales();
    }

    // ─── Construcción de la UI ────────────────────────────────────────────────

    private void construirUI() {
        setTop(crearHeader());
        setLeft(crearPanelIzquierdo());
        setCenter(crearPanelMapa());
        setBottom(crearStatusBar());
    }

    private HBox crearHeader() {
        HBox header = new HBox(10);
        header.setStyle(BG_HEADER + "-fx-padding:10 16; -fx-border-color:#21262d; -fx-border-width:0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("RouteFlow GPS");
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        logo.setTextFill(Color.web("#58a6ff"));

        Label sub = new Label(" — Delivery Antigua Guatemala");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web("#7d8590"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label reloj = new Label();
        reloj.setStyle(FG_GRAY + "-fx-font-size:13px;");
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1),
                e -> reloj.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();

        Button btnColapsar = new Button("◀");
        btnColapsar.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#58a6ff; " +
            "-fx-font-size:15px; -fx-cursor:hand; -fx-padding:0 4; -fx-border-width:0;");
        btnColapsar.setOnAction(e -> {
            boolean vis = panelIzquierdo.isVisible();
            panelIzquierdo.setVisible(!vis);
            panelIzquierdo.setManaged(!vis);
            btnColapsar.setText(vis ? "▶" : "◀");
        });

        header.getChildren().addAll(logo, sub, spacer, reloj, btnColapsar);
        return header;
    }

    private VBox crearPanelIzquierdo() {
        panelIzquierdo = new VBox();
        VBox panel = panelIzquierdo;
        panel.setPrefWidth(290);
        panel.setStyle(BG_PANEL + "-fx-border-color:#21262d; -fx-border-width:0 1 0 0;");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color:#161b22;");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        tabs.getTabs().addAll(
                crearTabRuta(),
                crearTabDelivery(),
                crearTabTrafico(),
                crearTabEditar()
        );

        panel.getChildren().add(tabs);
        return panel;
    }

    private Tab crearTabRuta() {
        Tab tab = new Tab("Ruta");

        comboOrigen  = comboBox("Ciudad origen");
        comboDestino = comboBox("Ciudad destino");

        spinnerHora = new Spinner<>(0, 23, LocalTime.now().getHour());
        spinnerHora.setEditable(true);
        spinnerHora.getStyleClass().add("dark-spinner");
        spinnerHora.getEditor().setStyle(
                "-fx-background-color:#21262d; -fx-text-fill:#e6edf3; -fx-prompt-text-fill:#7d8590;");
        spinnerHora.setPrefWidth(75);

        Button btnAhora = boton("Ahora", "#30363d");
        btnAhora.setPrefWidth(70);
        btnAhora.setOnAction(e -> spinnerHora.getValueFactory().setValue(LocalTime.now().getHour()));

        HBox horaRow = new HBox(6, spinnerHora, btnAhora);
        horaRow.setAlignment(Pos.CENTER_LEFT);

        Button btnCalc = boton("Calcular Ruta  →", "#1f6feb");
        btnCalc.setOnAction(e -> calcularRutaDirecta());

        lblResultadoRuta = new Label();
        lblResultadoRuta.setStyle(FG_GRAY + "-fx-font-size:11px; -fx-wrap-text:true;");
        lblResultadoRuta.setMaxWidth(230);

        VBox content = panelScroll(
                seccion("ORIGEN", comboOrigen),
                seccion("DESTINO", comboDestino),
                seccion("HORA SIMULADA", horaRow),
                btnCalc,
                lblResultadoRuta
        );

        tab.setContent(content);
        return tab;
    }

    private Tab crearTabDelivery() {
        Tab tab = new Tab("Delivery");

        comboCandidato = comboBox("Ciudad a agregar");

        Button btnAgregar = boton("+ Agregar parada", "#2ea043");
        btnAgregar.setOnAction(e -> {
            String sel = comboCandidato.getValue();
            if (sel != null && !ciudadesEntrega.contains(sel))
                ciudadesEntrega.add(sel);
        });

        listaEntregas = new ListView<>(ciudadesEntrega);
        listaEntregas.setPrefHeight(130);
        listaEntregas.getStyleClass().add("dark-list");
        listaEntregas.setStyle("-fx-background-color:#21262d; -fx-control-inner-background:#21262d; " +
                "-fx-border-color:#30363d; -fx-border-width:1; -fx-border-radius:5;");
        listaEntregas.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color:transparent;"); }
                else { setText(item); setStyle("-fx-background-color:#21262d; -fx-text-fill:#e6edf3; -fx-padding:5 8;"); }
            }
            @Override public void updateSelected(boolean sel) {
                super.updateSelected(sel);
                if (getItem() != null) setStyle(sel
                    ? "-fx-background-color:#1c3a6e; -fx-text-fill:#e6edf3; -fx-padding:5 8;"
                    : "-fx-background-color:#21262d; -fx-text-fill:#e6edf3; -fx-padding:5 8;");
            }
        });

        Button btnLimpiar = boton("✕ Limpiar lista", "#30363d");
        btnLimpiar.setOnAction(e -> ciudadesEntrega.clear());

        Button btnOptimizar = boton("Optimizar Ruta  →", "#e67e22");
        btnOptimizar.setOnAction(e -> calcularRutaDelivery());

        lblResultadoDelivery = new Label();
        lblResultadoDelivery.setStyle(FG_GRAY + "-fx-font-size:11px; -fx-wrap-text:true;");
        lblResultadoDelivery.setMaxWidth(230);

        VBox content = panelScroll(
                seccion("PUNTOS DE ENTREGA", comboCandidato, btnAgregar),
                listaEntregas,
                btnLimpiar,
                btnOptimizar,
                lblResultadoDelivery
        );

        tab.setContent(content);
        return tab;
    }

    private Tab crearTabTrafico() {
        Tab tab = new Tab("Tráfico");

        comboRuta = new ComboBox<>();
        comboRuta.setMaxWidth(Double.MAX_VALUE);
        estilizarComboBox(comboRuta, "Seleccionar ruta");
        comboRuta.setOnAction(e -> actualizarControlsTrafico());

        lblEstadoRuta = new Label("Selecciona una ruta arriba");
        lblEstadoRuta.setStyle(FG_GRAY + "-fx-font-size:11px; -fx-wrap-text:true;");
        lblEstadoRuta.setMaxWidth(230);

        sliderFactor = new Slider(1.0, 3.0, 1.0);
        sliderFactor.setMajorTickUnit(0.5);
        sliderFactor.setMinorTickCount(1);
        sliderFactor.setShowTickMarks(true);
        sliderFactor.setShowTickLabels(true);
        sliderFactor.getStyleClass().add("dark-slider");

        Label lblFactor = new Label("Factor: 1.0×  (1=fluido · 3=congestionado)");
        lblFactor.setStyle(FG_WHITE + "-fx-font-size:11px;");

        sliderFactor.valueProperty().addListener((obs, ov, nv) -> {
            lblFactor.setText(String.format("Factor: %.1f×  (1=fluido · 3=congestionado)", nv.doubleValue()));
            actualizarColorThumb(nv.doubleValue());
        });

        // Inicializar color del thumb cuando el nodo entre a la escena
        sliderFactor.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                Platform.runLater(() ->
                    Platform.runLater(() -> actualizarColorThumb(sliderFactor.getValue()))
                );
            }
        });

        btnBloquear = new ToggleButton("BLOQUEAR RUTA");
        btnBloquear.setMaxWidth(Double.MAX_VALUE);
        btnBloquear.setStyle("-fx-background-color:#30363d; -fx-text-fill:#e6edf3; -fx-cursor:hand;");
        btnBloquear.selectedProperty().addListener((obs, ov, nv) ->
                btnBloquear.setStyle("-fx-background-color:" + (nv ? "#6e1c1c" : "#30363d") +
                        "; -fx-text-fill:#e6edf3; -fx-cursor:hand;"));
        btnBloquear.selectedProperty().addListener((obs, ov, nv) ->
                btnBloquear.setText(nv ? "⛔  RUTA BLOQUEADA" : "BLOQUEAR RUTA"));

        Button btnAplicar = boton("Aplicar cambios", "#1f6feb");
        btnAplicar.setOnAction(e -> aplicarCambioTrafico());

        Label lblNota = new Label("Los cambios se reflejan en el próximo cálculo.");
        lblNota.setStyle(FG_GRAY + "-fx-font-size:10px; -fx-wrap-text:true;");
        lblNota.setMaxWidth(230);

        VBox content = panelScroll(
                seccion("SELECCIONAR RUTA", comboRuta),
                lblEstadoRuta,
                seccion("FACTOR DE TRÁFICO", sliderFactor, lblFactor),
                btnBloquear,
                btnAplicar,
                lblNota
        );

        tab.setContent(content);
        return tab;
    }

    private Tab crearTabEditar() {
        Tab tab = new Tab("Editar");

        Button btnCiudad = boton("+ Agregar Ciudad", "#8957e5");
        btnCiudad.setOnAction(e -> mostrarDialogoCiudad());

        Button btnRuta = boton("+ Agregar Conexión", "#8957e5");
        btnRuta.setOnAction(e -> mostrarDialogoRuta());

        Button btnGuardar = boton("💾  Guardar Mapa", "#2ea043");
        btnGuardar.setOnAction(e -> guardarMapa());

        Button btnImportar = boton("📂  Importar Mapa", "#30363d");
        btnImportar.setOnAction(e -> importarMapa());

        Label lblRuta = new Label("Guardado: data/antigua_map.txt");
        lblRuta.setStyle(FG_GRAY + "-fx-font-size:10px; -fx-wrap-text:true;");
        lblRuta.setMaxWidth(230);

        VBox content = panelScroll(
                seccion("NODOS", btnCiudad),
                seccion("ARISTAS", btnRuta),
                new Separator(),
                seccion("ARCHIVO", btnGuardar, btnImportar),
                lblRuta,
                new Separator(),
                crearSeccionPreferencias()
        );

        tab.setContent(content);
        return tab;
    }

    private StackPane crearPanelMapa() {
        mapView = new MapView();
        return new StackPane(mapView);
    }

    private HBox crearStatusBar() {
        HBox bar = new HBox();
        bar.setStyle(BG_STATUS + "-fx-padding:6 14;");
        bar.setAlignment(Pos.CENTER_LEFT);
        barraEstado = new Label("Iniciando RouteFlow GPS...");
        barraEstado.setStyle(FG_GRAY + "-fx-font-size:12px;");
        bar.getChildren().add(barraEstado);
        return bar;
    }

    // ─── Carga inicial ────────────────────────────────────────────────────────

    private void cargarDatosIniciales() {
        try {
            String ruta = "data/antigua_map.txt";
            if (!new File(ruta).exists()) {
                ruta = System.getProperty("user.dir") + "/data/antigua_map.txt";
            }
            grafo = DataManager.cargarDesdeArchivo(ruta);
            poblarComboBoxes();
            mapView.init(grafo);
            setEstado("Mapa cargado — " + grafo.getNodosCount()
                    + " nodos · " + grafo.getAristasCount() + " rutas");
        } catch (Exception e) {
            setEstado("Error al cargar el mapa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void poblarComboBoxes() {
        etiquetaAId.clear();
        etiquetaARuta.clear();
        List<String> etiquetas = new ArrayList<>();

        for (Ciudad c : grafo.getCiudades()) {
            String lbl = c.getNombre() + " (" + c.getId() + ")";
            etiquetas.add(lbl);
            etiquetaAId.put(lbl, c.getId());
        }
        etiquetas.sort(String::compareTo);

        ObservableList<String> items = FXCollections.observableArrayList(etiquetas);
        comboOrigen.setItems(items);
        comboDestino.setItems(items);
        comboCandidato.setItems(items);

        // Rutas para la pestaña de tráfico
        List<String> rutasLbls = new ArrayList<>();
        for (Ruta r : grafo.getRutas()) {
            Ciudad a = grafo.getCiudad(r.getOrigen());
            Ciudad b = grafo.getCiudad(r.getDestino());
            if (a == null || b == null) continue;
            String lbl = a.getNombre() + " → " + b.getNombre();
            rutasLbls.add(lbl);
            etiquetaARuta.put(lbl, new String[]{ r.getOrigen(), r.getDestino() });
        }
        rutasLbls.sort(String::compareTo);
        comboRuta.setItems(FXCollections.observableArrayList(rutasLbls));
    }

    // ─── Acciones ────────────────────────────────────────────────────────────

    private void calcularRutaDirecta() {
        String etOrigen  = comboOrigen.getValue();
        String etDestino = comboDestino.getValue();
        if (etOrigen == null || etDestino == null) {
            setEstado("⚠  Selecciona origen y destino.");
            return;
        }
        String origenId  = etiquetaAId.get(etOrigen);
        String destinoId = etiquetaAId.get(etDestino);
        if (origenId.equals(destinoId)) { setEstado("Origen y destino son iguales."); return; }

        int hora = getHoraSeleccionada();
        Dijkstra.ResultadoDijkstra res = Dijkstra.calcular(grafo, origenId, destinoId, hora);

        mapView.limpiarRutas();
        if (!res.encontrado) {
            setEstado("✗  No existe camino entre las ciudades seleccionadas.");
            lblResultadoRuta.setText("Sin ruta disponible.");
            lblResultadoRuta.setStyle(FG_RED + "-fx-font-size:11px; -fx-wrap-text:true;");
            return;
        }

        mapView.mostrarRuta(res.camino, grafo, colorCarroDirecto);
        mapView.animarVehiculo(res.camino, grafo, colorCarroDirecto);
        mapView.mostrarInfoRuta(res.distanciaKm, res.tiempoEstimadoMin,
                hora, res.camino.size() - 1, res.pasosDescripcion);

        String resumen = String.format("✓ %.2f km · ~%.0f min · %d pasos · hora %d:00",
                res.distanciaKm, res.tiempoEstimadoMin,
                res.camino.size() - 1, hora);
        lblResultadoRuta.setText(resumen);
        lblResultadoRuta.setStyle(FG_GREEN + "-fx-font-size:11px; -fx-wrap-text:true;");
        setEstado(String.format("Ruta directa: %.2f km · ~%.0f min · %s → %s",
                res.distanciaKm, res.tiempoEstimadoMin,
                grafo.getCiudad(origenId).getNombre(),
                grafo.getCiudad(destinoId).getNombre()));
    }

    private void calcularRutaDelivery() {
        if (ciudadesEntrega.isEmpty()) { setEstado("⚠  Agrega al menos una ciudad de entrega."); return; }

        String bodegaId = grafo.getCiudades().stream()
                .filter(Ciudad::isEsBodega).map(Ciudad::getId).findFirst().orElse(null);
        if (bodegaId == null) { setEstado("No hay bodega definida en el mapa."); return; }

        int hora = getHoraSeleccionada();
        FloydWarshall.MatrizDistancias matriz = FloydWarshall.calcular(grafo);

        List<String> pendientes = ciudadesEntrega.stream()
                .map(etiquetaAId::get).filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        List<String> rutaCompleta = new ArrayList<>();
        rutaCompleta.add(bodegaId);
        List<String> todosLosPasos = new ArrayList<>();
        double totalTiempo = 0;
        String posicion = bodegaId;

        while (!pendientes.isEmpty()) {
            String siguiente = matriz.getCiudadMasCercana(posicion, pendientes);
            pendientes.remove(siguiente);

            Dijkstra.ResultadoDijkstra tramo = Dijkstra.calcular(grafo, posicion, siguiente, hora);
            if (tramo.encontrado && tramo.camino.size() > 1) {
                rutaCompleta.addAll(tramo.camino.subList(1, tramo.camino.size()));
                todosLosPasos.addAll(tramo.pasosDescripcion);
                totalTiempo += tramo.tiempoEstimadoMin;
            }
            posicion = siguiente;
        }

        double distTotal = calcularDistanciaKm(rutaCompleta);

        mapView.limpiarRutas();
        mapView.mostrarRutaDelivery(rutaCompleta, grafo, colorCarroDelivery);
        mapView.animarVehiculo(rutaCompleta, grafo, colorCarroDelivery);
        mapView.mostrarInfoRuta(distTotal, totalTiempo, hora,
                ciudadesEntrega.size(), todosLosPasos);

        String resumen = String.format("✓ %d paradas · %.2f km · ~%.0f min",
                ciudadesEntrega.size(), distTotal, totalTiempo);
        lblResultadoDelivery.setText(resumen);
        lblResultadoDelivery.setStyle(FG_ORANGE + "-fx-font-size:11px; -fx-wrap-text:true;");
        setEstado(String.format("Delivery: %d paradas · %.2f km · ~%.0f min · hora %d:00",
                ciudadesEntrega.size(), distTotal, totalTiempo, hora));
    }

    private void actualizarControlsTrafico() {
        String lbl = comboRuta.getValue();
        if (lbl == null) return;
        String[] ids = etiquetaARuta.get(lbl);
        if (ids == null) return;

        List<Ruta> vecinos = grafo.getVecinos(ids[0]);
        Ruta ruta = vecinos.stream()
                .filter(r -> r.getDestino().equals(ids[1]))
                .findFirst().orElse(null);

        if (ruta == null) { lblEstadoRuta.setText("Ruta no encontrada."); return; }

        sliderFactor.setValue(ruta.getFactorTrafico());
        btnBloquear.setSelected(ruta.isBloqueado());

        String estado = ruta.isBloqueado() ? "⛔ BLOQUEADA" : "✓ ACTIVA";
        String tipo   = ruta.getTipoCamino().name();
        lblEstadoRuta.setText(String.format("%.1f km · x%.1f · %s · %s",
                ruta.getDistanciaKm(), ruta.getFactorTrafico(), tipo, estado));
        lblEstadoRuta.setStyle((ruta.isBloqueado() ? FG_RED : FG_GREEN) +
                "-fx-font-size:11px; -fx-wrap-text:true;");
    }

    private void aplicarCambioTrafico() {
        String lbl = comboRuta.getValue();
        if (lbl == null) { setEstado("⚠  Selecciona una ruta primero."); return; }
        String[] ids = etiquetaARuta.get(lbl);
        if (ids == null) return;

        double factor   = sliderFactor.getValue();
        boolean bloq    = btnBloquear.isSelected();

        grafo.setFactorTrafico(ids[0], ids[1], factor);
        grafo.setRutaBloqueada(ids[0], ids[1], bloq);

        actualizarControlsTrafico();
        setEstado(String.format("Tráfico actualizado: %s — x%.1f · %s",
                lbl, factor, bloq ? "BLOQUEADA" : "ACTIVA"));
    }

    private void mostrarDialogoCiudad() {
        Dialog<Ciudad> dlg = new Dialog<>();
        dlg.setTitle("Agregar Ciudad");
        dlg.setHeaderText("Nueva ciudad / punto de interés");
        aplicarEstiloOscuro(dlg.getDialogPane());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));
        g.setStyle("-fx-background-color:#161b22;");

        TextField tfId  = new TextField(); tfId.setPromptText("GT026");
        TextField tfNom = new TextField(); tfNom.setPromptText("Nombre del lugar");
        TextField tfLat = new TextField(); tfLat.setPromptText("14.5586");
        TextField tfLon = new TextField(); tfLon.setPromptText("-90.7295");
        CheckBox  cbBod = new CheckBox("Es bodega (origen de entregas)");

        g.add(new Label("ID:"),       0,0); g.add(tfId,  1,0);
        g.add(new Label("Nombre:"),   0,1); g.add(tfNom, 1,1);
        g.add(new Label("Latitud:"),  0,2); g.add(tfLat, 1,2);
        g.add(new Label("Longitud:"), 0,3); g.add(tfLon, 1,3);
        g.add(cbBod, 1, 4);

        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    return new Ciudad(tfId.getText().trim(), tfNom.getText().trim(),
                            Double.parseDouble(tfLat.getText()),
                            Double.parseDouble(tfLon.getText()),
                            cbBod.isSelected());
                } catch (Exception ex) {
                    setEstado("Error: latitud/longitud deben ser números.");
                }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(c -> {
            grafo.agregarCiudad(c);
            poblarComboBoxes();
            mapView.init(grafo);
            setEstado("Ciudad agregada: " + c.getNombre());
        });
    }

    private void mostrarDialogoRuta() {
        List<String> lbls = new ArrayList<>(etiquetaAId.keySet());
        lbls.sort(String::compareTo);
        ObservableList<String> opts = FXCollections.observableArrayList(lbls);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Agregar Conexión");
        dlg.setHeaderText("Nueva arista en el grafo");
        aplicarEstiloOscuro(dlg.getDialogPane());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));
        g.setStyle("-fx-background-color:#161b22;");

        ComboBox<String> cbO = new ComboBox<>(opts);
        ComboBox<String> cbD = new ComboBox<>(opts);
        cbO.setPrefWidth(200); estilizarComboBox(cbO, "Ciudad origen");
        cbD.setPrefWidth(200); estilizarComboBox(cbD, "Ciudad destino");

        CheckBox cbBidi = new CheckBox("Bidireccional");
        cbBidi.setSelected(true);

        ComboBox<Ruta.TipoCamino> cbTipo = new ComboBox<>(
                FXCollections.observableArrayList(Ruta.TipoCamino.values()));
        cbTipo.setValue(Ruta.TipoCamino.CARRETERA);
        estilizarComboBox(cbTipo, "Tipo de camino");

        g.add(new Label("Origen:"),     0,0); g.add(cbO,    1,0);
        g.add(new Label("Destino:"),    0,1); g.add(cbD,    1,1);
        g.add(new Label("Tipo camino:"),0,2); g.add(cbTipo, 1,2);
        g.add(cbBidi, 1, 3);

        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.setResultConverter(bt -> bt);

        Optional<ButtonType> resultado = dlg.showAndWait();
        if (!resultado.isPresent() || resultado.get() != ButtonType.OK) return;
        if (cbO.getValue() == null || cbD.getValue() == null) {
            setEstado("⚠  Selecciona origen y destino.");
            return;
        }
        String origenId  = etiquetaAId.get(cbO.getValue());
        String destinoId = etiquetaAId.get(cbD.getValue());
        if (origenId == null || destinoId == null) return;
        try {
            grafo.agregarRuta(origenId, destinoId, cbBidi.isSelected(), 1.0, cbTipo.getValue());
            poblarComboBoxes();
            mapView.init(grafo);
            setEstado("Conexión agregada: " + cbO.getValue() + " → " + cbD.getValue());
        } catch (Exception ex) {
            setEstado("Error al agregar conexión: " + ex.getMessage());
        }
    }

    private void importarMapa() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar Mapa RouteFlow");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de mapa (*.txt)", "*.txt"));
        File archivo = fc.showOpenDialog(stage);
        if (archivo == null) return;
        try {
            grafo = DataManager.cargarDesdeArchivo(archivo.getAbsolutePath());
            poblarComboBoxes();
            mapView.init(grafo);
            setEstado("Mapa importado: " + archivo.getName()
                    + " — " + grafo.getNodosCount() + " nodos · " + grafo.getAristasCount() + " rutas");
        } catch (Exception ex) {
            setEstado("Error al importar: " + ex.getMessage());
        }
    }

    private VBox crearSeccionPreferencias() {
        // ── Color del carro ──────────────────────────────────
        ColorPicker cpDirecto = new ColorPicker(Color.web(colorCarroDirecto));
        cpDirecto.setMaxWidth(Double.MAX_VALUE);
        cpDirecto.setStyle("-fx-background-color:#21262d; -fx-border-color:#30363d; " +
                "-fx-border-radius:5; -fx-border-width:1;");
        cpDirecto.setOnAction(e -> colorCarroDirecto = toHex(cpDirecto.getValue()));

        ColorPicker cpDelivery = new ColorPicker(Color.web(colorCarroDelivery));
        cpDelivery.setMaxWidth(Double.MAX_VALUE);
        cpDelivery.setStyle("-fx-background-color:#21262d; -fx-border-color:#30363d; " +
                "-fx-border-radius:5; -fx-border-width:1;");
        cpDelivery.setOnAction(e -> colorCarroDelivery = toHex(cpDelivery.getValue()));

        // ── Estilo de línea ──────────────────────────────────
        String estNormal = "-fx-background-color:#21262d; -fx-text-fill:#e6edf3; " +
                "-fx-border-color:#30363d; -fx-border-radius:4; -fx-cursor:hand; -fx-font-size:11px;";
        String estSelec  = "-fx-background-color:#1f6feb; -fx-text-fill:#ffffff; " +
                "-fx-border-color:#1f6feb; -fx-border-radius:4; -fx-cursor:hand; -fx-font-size:11px;";

        ToggleButton btnSolida   = new ToggleButton("Sólida");
        ToggleButton btnPunteada = new ToggleButton("Punteada");
        ToggleGroup  tg          = new ToggleGroup();
        btnSolida.setToggleGroup(tg);
        btnPunteada.setToggleGroup(tg);
        btnSolida.setMaxWidth(Double.MAX_VALUE);
        btnPunteada.setMaxWidth(Double.MAX_VALUE);
        btnSolida.setSelected(true);
        btnSolida.setStyle(estSelec);
        btnPunteada.setStyle(estNormal);

        tg.selectedToggleProperty().addListener((obs, ov, nv) -> {
            if (nv == null) { tg.selectToggle(ov); return; }
            boolean sol = (nv == btnSolida);
            btnSolida.setStyle(sol  ? estSelec : estNormal);
            btnPunteada.setStyle(sol ? estNormal : estSelec);
            estiloRutaActual = sol ? "solido" : "punteado";
            mapView.setEstiloRuta(estiloRutaActual);
        });

        HBox estiloRow = new HBox(6, btnSolida, btnPunteada);
        HBox.setHgrow(btnSolida,   Priority.ALWAYS);
        HBox.setHgrow(btnPunteada, Priority.ALWAYS);

        return new VBox(6,
                seccion("COLOR CARRO — RUTA DIRECTA",  cpDirecto),
                seccion("COLOR CARRO — DELIVERY",       cpDelivery),
                seccion("ESTILO DE LÍNEA DE RUTA",      estiloRow));
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }

    private void guardarMapa() {
        try {
            DataManager.guardarEnArchivo(grafo, "data/antigua_map.txt");
            setEstado("Mapa guardado en data/antigua_map.txt");
        } catch (Exception e) {
            setEstado("Error al guardar: " + e.getMessage());
        }
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    private int getHoraSeleccionada() {
        try { return spinnerHora.getValue(); }
        catch (Exception e) { return LocalTime.now().getHour(); }
    }

    private double calcularDistanciaKm(List<String> camino) {
        double total = 0;
        for (int i = 0; i < camino.size() - 1; i++) {
            Ciudad a = grafo.getCiudad(camino.get(i));
            Ciudad b = grafo.getCiudad(camino.get(i + 1));
            if (a != null && b != null)
                total += Haversine.calcularDistanciaKm(
                        a.getLatitud(), a.getLongitud(), b.getLatitud(), b.getLongitud());
        }
        return total;
    }

    private void setEstado(String msg) {
        barraEstado.setText(msg);
    }

    // ─── Helpers de UI ───────────────────────────────────────────────────────

    private Button boton(String texto, String colorHex) {
        Button b = new Button(texto);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color:" + colorHex +
                "; -fx-text-fill:#e6edf3; -fx-font-size:12px; -fx-cursor:hand;" +
                "-fx-background-radius:5; -fx-padding:6 10;");
        return b;
    }

    private ComboBox<String> comboBox(String prompt) {
        ComboBox<String> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);
        estilizarComboBox(cb, prompt);
        return cb;
    }

    /**
     * Aplica estilos oscuros a cualquier ComboBox usando cell factories (no CSS),
     * porque Modena override los selectores de popup con mayor especificidad.
     */
    private <T> void estilizarComboBox(ComboBox<T> cb, String promptText) {
        cb.getStyleClass().add("dark-combo");
        cb.setStyle("-fx-background-color:#21262d; -fx-border-color:#30363d; " +
                "-fx-border-radius:5; -fx-background-radius:5; -fx-border-width:1;");

        // Celda del botón: muestra prompt en gris o item seleccionado en blanco
        cb.setButtonCell(new ListCell<T>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setBackground(javafx.scene.layout.Background.EMPTY);
                if (empty || item == null) {
                    setText(promptText);
                    setStyle("-fx-text-fill:#7d8590;");
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill:#e6edf3;");
                }
            }
        });

        // Celdas del dropdown
        cb.setCellFactory(lv -> new ListCell<T>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color:#21262d;"); }
                else { setText(item.toString()); estiloNormal(); }
            }
            @Override public void updateSelected(boolean sel) {
                super.updateSelected(sel);
                if (getItem() != null) { if (sel) estiloSeleccionado(); else estiloNormal(); }
            }
            private void estiloNormal() {
                setStyle("-fx-background-color:#21262d; -fx-text-fill:#e6edf3; -fx-padding:5 10;");
            }
            private void estiloSeleccionado() {
                setStyle("-fx-background-color:#1f6feb; -fx-text-fill:#ffffff; -fx-padding:5 10;");
            }
        });
    }

    /** Aplica el stylesheet oscuro a un DialogPane. */
    private void aplicarEstiloOscuro(DialogPane pane) {
        URL css = getClass().getResource("/com/routeflow/style.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());
        pane.setStyle("-fx-background-color:#161b22;");
    }

    /**
     * Interpola el color del thumb del slider entre azul (1.0) y rojo clásico (3.0).
     * Se llama al entrar a escena y en cada cambio de valor.
     * La forma de carro viene del CSS (.dark-slider .thumb { -fx-shape: ... }).
     */
    private void actualizarColorThumb(double valor) {
        Node thumb = sliderFactor.lookup(".thumb");
        if (thumb == null) return;
        double t = Math.max(0, Math.min(1, (valor - 1.0) / 2.0));
        // Azul #1f6feb → Rojo clásico #b91c1c
        int r = (int) (31  + t * (185 - 31));
        int g = (int) (111 + t * (28  - 111));
        int b = (int) (235 + t * (28  - 235));
        thumb.setStyle(String.format("-fx-background-color: rgb(%d,%d,%d);", r, g, b));
    }

    private Label seccionLabel(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill:#7d8590; -fx-font-size:10px; -fx-font-weight:bold; -fx-padding:6 0 2 0;");
        return l;
    }

    private VBox seccion(String titulo, Node... nodos) {
        VBox vb = new VBox(4);
        vb.getChildren().add(seccionLabel(titulo));
        vb.getChildren().addAll(nodos);
        return vb;
    }

    private VBox panelScroll(Node... nodos) {
        VBox inner = new VBox(8);
        inner.setPadding(new Insets(10));
        inner.setStyle("-fx-background-color:#161b22;");
        inner.getChildren().addAll(nodos);

        ScrollPane sp = new ScrollPane(inner);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#161b22; -fx-background-color:#161b22; " +
                "-fx-border-width:0; -fx-padding:0;");

        VBox wrapper = new VBox(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        wrapper.setStyle("-fx-background-color:#161b22;");
        return wrapper;
    }
}