# Guía de Extensión — RouteFlow GPS

Cómo agregar nuevas funciones al proyecto sin romper lo que ya existe.

---

## Agregar un nuevo algoritmo

1. Crear `src/main/java/com/routeflow/algorithm/MiAlgoritmo.java`
2. Seguir el patrón de Dijkstra: método estático `calcular(Grafo, ...)` que retorna
   un objeto resultado inmutable.
3. Llamarlo desde `MainWindow` con un nuevo botón.

Ejemplo — A* (más eficiente que Dijkstra con heurística geográfica):
```java
public class AStar {
    public static ResultadoDijkstra calcular(Grafo grafo, String origen, 
                                              String destino) {
        // Igual que Dijkstra pero la prioridad = distancia_acumulada + heuristica
        // heuristica = Haversine(nodoActual, destino)
    }
}
```

---

## Agregar un nuevo tipo de marcador en el mapa

1. En `map.html`, agregar el CSS del nuevo tipo:
```css
.icono-almacen {
    background: #9b59b6;
    border-radius: 4px;
    width: 24px; height: 24px;
}
```

2. En la función `crearIcono()` de `map.html`:
```javascript
if (tipo === 'almacen') cssClass = 'icono-almacen';
```

3. En `MapView.java`, agregar el método que lo llama:
```java
public void mostrarAlmacen(Ciudad c) {
    ejecutarJS(String.format("agregarMarcador('%s','%s',%f,%f,'almacen')",
        c.getId(), c.getNombre(), c.getLatitud(), c.getLongitud()));
}
```

---

## Agregar un nuevo campo al archivo de datos

Si quieres agregar `velocidadMaxKmH` a las rutas:

1. Cambiar `Ruta.java`: agregar el campo y el constructor.
2. Cambiar `DataManager.cargarDesdeArchivo()`:
```java
// partes[4] = velocidadMaxKmH (nuevo campo)
double vel = partes.length > 4 ? Double.parseDouble(partes[4]) : 40.0;
```
   El valor por defecto 40.0 mantiene compatibilidad con archivos viejos.
3. Cambiar `DataManager.guardarEnArchivo()` para incluir el nuevo campo.
4. Actualizar `antigua_map.txt` con el nuevo formato.

---

## Comunicación JavaScript → Java

Para que el mapa responda a clicks del usuario:

En `map.html`:
```javascript
mapa.on('click', function(e) {
    // Llamar al objeto Java registrado desde MapView
    if (window.javaApp) {
        window.javaApp.onMapClick(e.latlng.lat, e.latlng.lng);
    }
});
```

En `MapView.java`:
```java
// Clase interna que expone métodos a JavaScript
public class JavaBridge {
    public void onMapClick(double lat, double lon) {
        Platform.runLater(() -> {
            // Hacer algo con las coordenadas en el hilo de JavaFX
        });
    }
}

// En el listener de SUCCEEDED:
JSObject window = (JSObject) engine.executeScript("window");
window.setMember("javaApp", new JavaBridge());
```

---

## Ejecutar tests unitarios

Crear `src/test/java/com/routeflow/algorithm/DijkstraTest.java`:
```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DijkstraTest {
    @Test
    void rutaDirectaEncuentraCamino() {
        Grafo g = new Grafo();
        g.agregarCiudad(new Ciudad("A", "Ciudad A", 14.55, -90.72, true));
        g.agregarCiudad(new Ciudad("B", "Ciudad B", 14.52, -90.76, false));
        g.agregarRuta("A", "B", true);
        
        var resultado = Dijkstra.calcular(g, "A", "B");
        
        assertTrue(resultado.encontrado);
        assertEquals(List.of("A", "B"), resultado.camino);
    }
}
```

Agregar al `pom.xml`:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

Ejecutar: `mvn test`