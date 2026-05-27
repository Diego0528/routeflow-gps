package com.routeflow.algorithm;

import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import com.routeflow.model.Ruta;

import java.util.*;

/**
 * Algoritmo de Dijkstra para encontrar el camino más corto entre dos nodos.
 *
 * La cola de prioridad está implementada MANUALMENTE con un ArrayList ordenado
 * para que los estudiantes vean cómo funciona el mecanismo internamente,
 * sin depender de PriorityQueue de Java.
 */
public class Dijkstra {

    /**
     * Nodo interno de la cola de prioridad manual.
     * Almacena el id del nodo y la distancia acumulada hasta él.
     */
    private static class NodoPrioridad {
        String id;
        double distancia;

        NodoPrioridad(String id, double distancia) {
            this.id = id;
            this.distancia = distancia;
        }
    }

    /**
     * Resultado completo del cálculo de Dijkstra.
     */
    public static class ResultadoDijkstra {
        public final List<String> camino;           // ids de nodos en orden
        public final double distanciaTotal;          // en kilómetros
        public final List<String> pasosDescripcion;  // texto legible del recorrido
        public final boolean encontrado;

        public ResultadoDijkstra(List<String> camino, double distanciaTotal,
                                  List<String> pasosDescripcion, boolean encontrado) {
            this.camino = camino;
            this.distanciaTotal = distanciaTotal;
            this.pasosDescripcion = pasosDescripcion;
            this.encontrado = encontrado;
        }
    }

    /**
     * Calcula el camino más corto desde un nodo origen hasta un destino.
     * Retorna un ResultadoDijkstra con encontrado=false si no hay camino.
     */
    public static ResultadoDijkstra calcular(Grafo grafo, String origenId, String destinoId) {
        // distancias[id] = menor distancia conocida desde origen hasta ese nodo
        Map<String, Double> distancias = new HashMap<>();
        // predecesor[id] = nodo desde el que llegamos con la ruta más corta
        Map<String, String> predecesor = new HashMap<>();
        // Cola de prioridad manual: lista ordenada por distancia ascendente
        List<NodoPrioridad> colaPrioridad = new ArrayList<>();

        // Inicializar todas las distancias a infinito
        for (Ciudad c : grafo.getCiudades()) {
            distancias.put(c.getId(), Double.MAX_VALUE);
        }
        distancias.put(origenId, 0.0);
        colaPrioridad.add(new NodoPrioridad(origenId, 0.0));

        Set<String> procesados = new HashSet<>();

        while (!colaPrioridad.isEmpty()) {
            // Extraer el nodo con menor distancia (mínimo de la lista)
            NodoPrioridad actual = extraerMinimo(colaPrioridad);

            if (procesados.contains(actual.id)) continue;
            procesados.add(actual.id);

            if (actual.id.equals(destinoId)) break;

            for (Ruta vecino : grafo.getVecinos(actual.id)) {
                double nuevaDistancia = distancias.get(actual.id) + vecino.getDistanciaKm();
                if (nuevaDistancia < distancias.getOrDefault(vecino.getDestino(), Double.MAX_VALUE)) {
                    distancias.put(vecino.getDestino(), nuevaDistancia);
                    predecesor.put(vecino.getDestino(), actual.id);
                    colaPrioridad.add(new NodoPrioridad(vecino.getDestino(), nuevaDistancia));
                }
            }
        }

        // Reconstruir el camino desde destino hacia origen siguiendo predecesores
        if (!predecesor.containsKey(destinoId) && !origenId.equals(destinoId)) {
            return new ResultadoDijkstra(Collections.emptyList(), 0, Collections.emptyList(), false);
        }

        List<String> camino = reconstruirCamino(predecesor, origenId, destinoId);
        double distanciaTotal = distancias.get(destinoId);
        List<String> pasos = generarPasos(camino, grafo, distanciaTotal);

        return new ResultadoDijkstra(camino, distanciaTotal, pasos, true);
    }

    /** Extrae y elimina el NodoPrioridad con menor distancia de la lista. */
    private static NodoPrioridad extraerMinimo(List<NodoPrioridad> lista) {
        int indiceMinimo = 0;
        for (int i = 1; i < lista.size(); i++) {
            if (lista.get(i).distancia < lista.get(indiceMinimo).distancia) {
                indiceMinimo = i;
            }
        }
        return lista.remove(indiceMinimo);
    }

    /** Sigue el mapa de predecesores para reconstruir el camino en orden correcto. */
    private static List<String> reconstruirCamino(Map<String, String> predecesor,
                                                    String origenId, String destinoId) {
        LinkedList<String> camino = new LinkedList<>();
        String actual = destinoId;
        while (actual != null) {
            camino.addFirst(actual);
            actual = predecesor.get(actual);
        }
        return camino;
    }

    /** Genera descripciones textuales de cada paso del camino. */
    private static List<String> generarPasos(List<String> camino, Grafo grafo,
                                               double distanciaTotal) {
        List<String> pasos = new ArrayList<>();
        for (int i = 0; i < camino.size() - 1; i++) {
            Ciudad desde = grafo.getCiudad(camino.get(i));
            Ciudad hasta = grafo.getCiudad(camino.get(i + 1));
            double dist = Haversine.calcularDistanciaKm(
                    desde.getLatitud(), desde.getLongitud(),
                    hasta.getLatitud(), hasta.getLongitud());
            pasos.add(String.format("  %d. %s → %s (%.2f km)",
                    i + 1, desde.getNombre(), hasta.getNombre(), dist));
        }
        pasos.add(String.format("  Distancia total: %.2f km", distanciaTotal));
        return pasos;
    }
}