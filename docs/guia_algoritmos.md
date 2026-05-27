# Guía de Algoritmos — RouteFlow GPS

Material de referencia para estudiantes que deseen entender o modificar
los algoritmos implementados en el proyecto.

---

## Haversine — Distancia entre coordenadas GPS

### El problema
Dos ciudades tienen coordenadas (lat, lon). ¿Cuántos kilómetros las separan
sobre la superficie de la Tierra?

No se puede usar Pitágoras porque la Tierra es (aproximadamente) una esfera.

### La fórmula

```
dLat = (lat2 - lat1) en radianes
dLon = (lon2 - lon1) en radianes

a = sin²(dLat/2) + cos(lat1) · cos(lat2) · sin²(dLon/2)
c = 2 · atan2(√a, √(1-a))
d = 6371 · c   ← kilómetros
```

### Ejemplo concreto

GT001 (Antigua) → GT002 (Ciudad Vieja):
- Antigua:     lat=14.5586, lon=-90.7295
- Ciudad Vieja: lat=14.5224, lon=-90.7631

```java
double d = Haversine.calcularDistanciaKm(14.5586, -90.7295, 14.5224, -90.7631);
// Resultado aproximado: ~5.2 km
```

---

## Dijkstra — Camino más corto de un origen a un destino

### Intuición
Imagina que el origen emite una "ola de agua". La ola se expande hacia afuera,
llegando primero a los nodos más cercanos. Cuando la ola llega al destino,
el camino recorrido es el más corto.

### Pseudocódigo

```
distancias[origen] = 0
distancias[todos los demás] = ∞
cola = { origen }

mientras cola no vacía:
    actual = extraer nodo con menor distancia de la cola
    si actual == destino: terminar

    para cada vecino de actual:
        nuevaDist = distancias[actual] + peso(actual, vecino)
        si nuevaDist < distancias[vecino]:
            distancias[vecino] = nuevaDist
            predecesor[vecino] = actual
            agregar vecino a la cola

camino = reconstruir desde destino siguiendo predecesores
```

### Reconstrucción del camino

```
camino = []
actual = destino
mientras actual != null:
    camino.addFirst(actual)
    actual = predecesor[actual]
```

### Complejidad
| Estructura de cola | Tiempo     | Espacio  |
|--------------------|------------|----------|
| Lista (este código)| O(V²)      | O(V + E) |
| Heap binario       | O((V+E)logV)| O(V + E) |
| Heap de Fibonacci  | O(E + V logV)| O(V)   |

Para V=10 nodos, cualquier implementación es instantánea.

---

## Floyd-Warshall — Todas las distancias mínimas

### Cuándo usar Floyd-Warshall vs Dijkstra

| Necesito...                    | Usar          |
|--------------------------------|---------------|
| Una ruta específica A → B      | Dijkstra      |
| Todas las rutas entre todos    | Floyd-Warshall|
| Muchas rutas desde un origen   | Dijkstra (una vez) |
| Delivery con N destinos        | Floyd-Warshall (precalcular) |

### El truco del nodo intermediario

Para mejorar la distancia entre i y j, preguntamos:
"¿Es más corto pasar por el nodo k?"

```
si dist[i][k] + dist[k][j] < dist[i][j]:
    dist[i][j] = dist[i][k] + dist[k][j]
```

Probamos esto para TODOS los posibles intermediarios k.

### Inicialización de la matriz

```
dist[i][i] = 0           ← un nodo a sí mismo tiene distancia 0
dist[i][j] = peso(i,j)   ← si existe la arista
dist[i][j] = ∞           ← si no hay conexión directa
```

### Ejemplo con 3 nodos

```
Antes de Floyd-Warshall:
    A    B    C
A [ 0    5   ∞ ]
B [ 5    0    3 ]
C [ ∞    3    0 ]

Después (k=B como intermediario):
    A    B    C
A [ 0    5    8 ]   ← A→B→C = 5+3 = 8
B [ 5    0    3 ]
C [ 8    3    0 ]   ← C→B→A = 3+5 = 8
```

---

## Heurística Greedy de Delivery

### El problema del viajante (TSP)
Dado un conjunto de ciudades a visitar, encontrar el recorrido más corto
que pase por todas es NP-difícil: no existe algoritmo eficiente conocido
para resolverlo exactamente.

### Vecino más cercano (Nearest Neighbor)
Heurística simple que da resultados razonables:

```
posicion = bodega
mientras haya entregas pendientes:
    siguiente = ciudad más cercana a posicion
    ir de posicion a siguiente (usando Dijkstra para el camino real)
    posicion = siguiente
```

**Garantía:** ninguna. Puede estar hasta un 25% lejos del óptimo en casos malos.  
**Ventaja:** O(n²) con la matriz precalculada de Floyd-Warshall.

### Por qué combinamos Floyd-Warshall con Dijkstra
- Floyd-Warshall da la distancia mínima entre todos los pares en O(1) lookup.
- Pero Floyd-Warshall no guarda el camino, solo la distancia.
- Por eso usamos Dijkstra para calcular el camino real de cada tramo.

---

## Búsqueda BFS (en Grafo.existeCamino)

BFS (Breadth-First Search) verifica si existe algún camino entre dos nodos
sin preocuparse por el peso de las aristas.

```
cola = [origen]
visitados = {origen}

mientras cola no vacía:
    actual = extraer de la cola
    si actual == destino: retornar true
    para cada vecino de actual:
        si vecino no visitado:
            marcar como visitado
            agregar a cola

retornar false
```

Útil antes de lanzar Dijkstra para evitar búsquedas inútiles en grafos
desconectados.

---

## Complejidades resumen

| Algoritmo      | Tiempo    | Espacio | Uso en RouteFlow                |
|----------------|-----------|---------|---------------------------------|
| Haversine      | O(1)      | O(1)    | Peso de cada arista             |
| BFS            | O(V+E)    | O(V)    | Verificar conectividad          |
| Dijkstra       | O(V²)     | O(V+E)  | Ruta directa A→B                |
| Floyd-Warshall | O(V³)     | O(V²)   | Precálculo delivery             |
| Greedy NN      | O(n²)     | O(1)    | Orden de visita en delivery     |

V = vértices (ciudades), E = aristas (rutas), n = entregas pendientes