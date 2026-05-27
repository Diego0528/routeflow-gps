# RouteFlow GPS

Simulador de rutas de delivery para Antigua Guatemala.  
Proyecto de estudio en Java 21 + JavaFX + Maven.

## Requisitos

| Herramienta | Versión mínima |
|-------------|----------------|
| Java JDK    | 21             |
| Maven       | 3.8+           |
| IntelliJ IDEA | Cualquier edición |

## Ejecutar

```bash
mvn clean compile
mvn javafx:run
```

O desde IntelliJ: abrir como proyecto Maven → ejecutar `Main.java`.

## Estructura

```
routeflow-gps/
├── data/antigua_map.txt        ← datos del mapa (editable a mano)
├── docs/                       ← documentación e informe técnico
├── src/main/java/com/routeflow/
│   ├── Main.java               ← punto de entrada JavaFX
│   ├── model/                  ← Ciudad, Ruta, Grafo
│   ├── algorithm/              ← Dijkstra, Floyd-Warshall, Haversine
│   ├── data/                   ← lectura/escritura de archivos
│   └── ui/                     ← ventana JavaFX + mapa Leaflet
└── src/main/resources/
    └── com/routeflow/map.html  ← HTML autocontenido con Leaflet.js
```

## Uso rápido

1. **Ruta directa**: selecciona origen y destino en los ComboBox → "Calcular Ruta Directa"
2. **Delivery**: agrega ciudades al ListView → "Optimizar Ruta Delivery"
3. **Editar mapa**: usa los botones "Agregar Ciudad / Ruta" → "Guardar Mapa"

## Documentación completa

Ver [`docs/informe_tecnico.md`](docs/informe_tecnico.md) para la descripción detallada de cada componente, decisiones de diseño y guía de extensión.