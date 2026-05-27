# Informe Técnico — RouteFlow GPS

**Proyecto:** Simulador GPS de delivery en Antigua Guatemala  
**Stack:** Java 21 · JavaFX 21 · Maven 3.8+  
**Fecha:** Mayo 2026

---

## 1. Descripción general

RouteFlow GPS es una aplicación de escritorio que visualiza un grafo de ciudades del
departamento de Sacatepéquez (Guatemala) sobre un mapa interactivo de Leaflet.js,
y permite calcular rutas óptimas usando Dijkstra y Floyd-Warshall.

El objetivo pedagógico es que estudiantes universitarios lean, entiendan y extiendan
el código fuente. Por eso cada clase tiene una sola responsabilidad, los algoritmos
están implementados desde cero (sin librerías externas) y el formato de datos es
texto plano editable a mano.

---

## 2. Arquitectura en capas

```
┌─────────────────────────────────────┐
│           ui/  (JavaFX)             │  MainWindow · MapView
├─────────────────────────────────────┤
│        algorithm/  (puro Java)      │  Dijkstra · FloydWarshall · Haversine
├─────────────────────────────────────┤
│         model/  (dominio)           │  Ciudad · Ruta · Grafo
├─────────────────────────────────────┤
│         data/  (persistencia)       │  DataManager
└─────────────────────────────────────┘
```

Las capas solo dependen hacia abajo: la UI no llama a DataManager directamente,
y los algoritmos no conocen nada de JavaFX.

---

## 3. Modelo de datos

### 3.1 Ciudad (nodo del grafo)

| Campo       | Tipo    | Descripción                                  |
|-------------|---------|----------------------------------------------|
| `id`        | String  | Clave única (ej. `GT001`)                    |
| `nombre`    | String  | Nombre legible del lugar                     |
| `latitud`   | double  | Coordenada GPS decimal (WGS84)               |
| `longitud`  | double  | Coordenada GPS decimal (WGS84)               |
| `esBodega`  | boolean | `true` = punto de origen de los pedidos      |

### 3.2 Ruta (arista del grafo)

| Campo             | Tipo    | Descripción                                       |
|-------------------|---------|---------------------------------------------------|
| `origen`          | String  | ID de la ciudad de partida                        |
| `destino`         | String  | ID de la ciudad de llegada                        |
| `distanciaKm`     | double  | Calculada por Haversine (no se guarda en archivo) |
| `esBidireccional` | boolean | Si `true`, también existe la arista inversa       |

### 3.3 Grafo (lista de adyacencia)

Estructura interna:
```
ciudades:    Map<String, Ciudad>       → todos los nodos
adyacencia:  Map<String, List<Ruta>>  → lista de adyacencia
```

La decisión de usar listas de adyacencia (en lugar de matriz de adyacencia) se debe
a que el grafo de Antigua es **disperso**: la mayoría de los nodos tiene 2-4 vecinos,
no conexiones a todos los demás, por lo que la matriz desperdiciaría memoria.

---

## 4. Algoritmos implementados

### 4.1 Haversine

**Propósito:** calcular la distancia real entre dos puntos GPS sobre la superficie terrestre.

**Por qué no distancia euclidiana:** para distancias cortas (< 100 km) la diferencia
es pequeña, pero Haversine da resultados correctos en cualquier escala y es el
estándar de la industria GIS.

```
a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlon/2)
c = 2·atan2(√a, √(1−a))
d = R·c        (R = 6371 km)
```

**Complejidad:** O(1) por par de puntos.

### 4.2 Dijkstra

**Propósito:** camino más corto entre un origen y un destino específico.

**Implementación:** cola de prioridad manual con `ArrayList` y extracción O(n).  
Para grafos pequeños (< 100 nodos) esto es aceptable; para grafos grandes
se reemplazaría por un heap binario o `PriorityQueue<>`.

**Flujo:**
1. Inicializar distancias a ∞, origen a 0.
2. Extraer el nodo no visitado con menor distancia acumulada.
3. Relajar todas sus aristas salientes.
4. Repetir hasta alcanzar el destino o agotar la cola.
5. Reconstruir el camino siguiendo el mapa de predecesores hacia atrás.

**Complejidad (con lista):** O(V²) en tiempo, O(V + E) en espacio.  
**Complejidad (con heap):** O((V + E) log V).

**Resultado:** `ResultadoDijkstra` contiene la lista de IDs del camino,
la distancia total y los pasos en texto legible.

### 4.3 Floyd-Warshall

**Propósito:** distancias mínimas entre **todos** los pares de nodos.

**Cuándo usarlo:** cuando se necesitan múltiples consultas origen-destino.
En el caso de delivery, calcular la matriz una sola vez es más eficiente
que ejecutar Dijkstra repetidamente.

**Núcleo del algoritmo (3 bucles anidados):**
```java
for (int k = 0; k < n; k++)          // nodo intermediario
  for (int i = 0; i < n; i++)
    for (int j = 0; j < n; j++)
      if (dist[i][k] + dist[k][j] < dist[i][j])
        dist[i][j] = dist[i][k] + dist[k][j];
```

**Complejidad:** O(n³) tiempo, O(n²) espacio.  
Para 10 nodos: 1000 operaciones — prácticamente instantáneo.

**Heurística greedy de delivery:**  
Dado un conjunto de entregas pendientes, Floyd-Warshall nos permite encontrar
la siguiente entrega más cercana en O(1) (lookup en la matriz). El algoritmo
resultante es "vecino más cercano" (Nearest Neighbor), que no garantiza la
solución óptima del problema del viajante (TSP es NP-difícil), pero da buenas
aproximaciones en la práctica.

---

## 5. Persistencia — Formato de archivo

Archivo: `data/antigua_map.txt`

```
# Comentarios con #
CIUDAD|id|nombre|latitud|longitud|esBodega
RUTA|idOrigen|idDestino|bidireccional
```

**Decisiones de diseño:**
- Separador `|` (no coma) para evitar conflictos con nombres que contienen comas.
- La distancia no se guarda: se recalcula con Haversine al cargar, garantizando
  consistencia con las coordenadas reales.
- Codificación UTF-8 para soportar caracteres especiales (tildes, ñ).
- Las rutas se procesan después de cargar todas las ciudades para evitar referencias
  a nodos aún no existentes.

---

## 6. Interfaz gráfica

### 6.1 MainWindow (BorderPane)

```
┌─────────────────────────────────────────────┐
│  TOP: Banner "RouteFlow GPS"                 │
├────────────┬────────────────────────────────┤
│ LEFT:      │ CENTER:                         │
│ Controles  │ MapView (WebView + Leaflet.js)  │
│ (220 px)   │                                 │
├────────────┴────────────────────────────────┤
│  BOTTOM: Barra de estado                     │
└─────────────────────────────────────────────┘
```

### 6.2 MapView (WebView + JavaScript)

La comunicación Java → JavaScript se hace con:
```java
engine.executeScript("funcionJS(argumentos)");
```

No hay comunicación JavaScript → Java en esta versión.  
Para implementarla en el futuro se puede usar `JSObject` y el mecanismo de
callbacks de WebView:
```java
JSObject window = (JSObject) engine.executeScript("window");
window.setMember("javaApp", miObjetoJava);
// En JS: window.javaApp.metodoJava(args)
```

### 6.3 Leaflet.js (map.html)

Cargado desde CDN: `https://unpkg.com/leaflet@1.9.4/dist/leaflet.js`

**Funciones JS disponibles desde Java:**

| Función | Descripción |
|---------|-------------|
| `agregarMarcador(id, nombre, lat, lon, tipo)` | Coloca un pin en el mapa |
| `dibujarRuta(puntosJson, color)` | Dibuja una polilínea |
| `limpiarMapa()` | Elimina rutas y vehículo |
| `moverVehiculo(lat, lon)` | Anima el marcador verde |
| `mostrarPopup(id, mensaje)` | Abre un popup en un marcador |

**Tipos de marcador:**
- `"bodega"` → ícono rojo (punto de origen)
- `"cliente"` → ícono azul (punto de entrega)
- `"vehiculo"` → ícono verde animado (vehículo en movimiento)

---

## 7. Flujos principales

### Flujo 1: Calcular ruta directa (Dijkstra)

```
Usuario selecciona origen + destino
  └─► MainWindow.calcularRutaDirecta()
        └─► Dijkstra.calcular(grafo, origenId, destinoId)
              └─► ResultadoDijkstra { camino, distancia, pasos }
        └─► MapView.mostrarRuta(camino, grafo)   → línea azul
        └─► MapView.animarRecorrido(camino, grafo) → marcador verde
        └─► barraEstado.setText(distancia + pasos)
```

### Flujo 2: Optimizar ruta delivery (Floyd-Warshall + greedy)

```
Usuario agrega ciudades al ListView
  └─► MainWindow.calcularRutaDelivery()
        └─► FloydWarshall.calcular(grafo) → MatrizDistancias
        └─► Bucle greedy:
              posicion = bodega
              mientras haya pendientes:
                siguiente = matriz.getCiudadMasCercana(posicion, pendientes)
                tramo = Dijkstra.calcular(grafo, posicion, siguiente)
                rutaCompleta += tramo.camino
                posicion = siguiente
        └─► MapView.mostrarRutaDelivery(rutaCompleta, grafo) → línea naranja
        └─► MapView.animarRecorrido(rutaCompleta, grafo)
```

---

## 8. Datos de prueba

10 ciudades del área metropolitana de Antigua Guatemala con coordenadas WGS84 reales:

| ID    | Ciudad                         | Bodega |
|-------|--------------------------------|--------|
| GT001 | Antigua Guatemala Centro       | Si     |
| GT002 | Ciudad Vieja                   | No     |
| GT003 | San Miguel Dueñas              | No     |
| GT004 | Alotenango                     | No     |
| GT005 | San Antonio Aguas Calientes    | No     |
| GT006 | Santa Catarina Barahona        | No     |
| GT007 | Jocotenango                    | No     |
| GT008 | San Felipe de Jesús            | No     |
| GT009 | Santiago Sacatepéquez          | No     |
| GT010 | San Lucas Sacatepéquez         | No     |

**Grafo de rutas (todas bidireccionales):**

```
GT001 ↔ GT002 ↔ GT003 ↔ GT004
  ↕       ↕
GT007   GT005 ↔ GT006 ↔ GT001
  ↕
GT008 ↔ GT009 ↔ GT010 ↔ GT001
```

El grafo es conexo: existe camino entre cualquier par de ciudades.

---

## 9. Posibles extensiones

| Extensión | Dificultad | Descripción |
|-----------|------------|-------------|
| Importar GeoJSON | Media | Cargar calles reales de OpenStreetMap |
| Tráfico en tiempo real | Alta | Ajustar pesos de aristas según hora del día |
| Backend REST | Alta | Separar lógica en servidor Spring Boot |
| Comunicación JS → Java | Media | Usar JSObject para clicks en el mapa |
| Algoritmo A* | Media | Más eficiente que Dijkstra con heurística de distancia |
| TSP exacto | Alta | Branch and bound para delivery óptimo (NP-duro) |
| Múltiples vehículos | Alta | Vrp (Vehicle Routing Problem) |

---

## 10. Guía de instalación

### Requisito previo: Java 21

```bash
java -version
# Debe mostrar: openjdk 21.x.x
```

### Instalar Maven (si no está instalado)

**Windows:**
1. Descargar Maven desde https://maven.apache.org/download.cgi
2. Extraer en `C:\Program Files\Apache\maven`
3. Agregar `C:\Program Files\Apache\maven\bin` al PATH del sistema
4. Verificar: `mvn -version`

**O usar Maven embebido de IntelliJ IDEA:**
- File → Settings → Build Tools → Maven → Maven home path: `(Bundled)`

### Compilar y ejecutar

```bash
# Desde la carpeta raíz del proyecto
mvn clean compile
mvn javafx:run
```

### Desde IntelliJ IDEA

1. File → Open → seleccionar la carpeta `routeflow-gps`
2. Esperar que Maven descargue las dependencias (~200 MB primera vez)
3. Clic derecho en `Main.java` → Run 'Main.main()'

---

## 11. Errores comunes y soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `Error opening registry key` | JavaFX no encuentra módulos | Usar `mvn javafx:run` en vez de ejecutar el JAR directamente |
| `No se encontró map.html` | Archivo no está en `target/classes` | Verificar que `src/main/resources/com/routeflow/map.html` existe |
| El mapa no carga | Sin conexión a internet | Leaflet se carga desde CDN; necesita internet |
| `Ciudad no encontrada` al agregar ruta | ID no existe en el grafo | Verificar que las ciudades estén definidas antes que las rutas en el archivo |
| El mapa Leaflet no muestra los marcadores | WebView cargó antes que el listener | Normal al inicio — los marcadores aparecen cuando el estado del Worker cambia a SUCCEEDED |

---

*Proyecto creado como material de estudio para cursos de Estructuras de Datos y Algoritmos.*