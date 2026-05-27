package com.routeflow.algorithm;

import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import com.routeflow.model.Ruta;

import java.util.*;

/**
 * Algoritmo de Floyd-Warshall para calcular las distancias mínimas entre
 * TODOS los pares de nodos del grafo en una sola ejecución.
 *
 * Ventaja sobre Dijkstra: si necesitamos saber la distancia entre muchos pares,
 * Floyd-Warshall es más eficiente porque lo precalcula todo de una vez.
 * La heurística greedy de delivery usa estos resultados para ordenar las entregas.
 *
 * Complejidad: O(n³) en tiempo, O(n²) en espacio.
 */
public class FloydWarshall {

    /**
     * Resultado del algoritmo: la matriz completa de distancias mínimas
     * entre todos los pares de ciudades del grafo.
     */
    public static class MatrizDistancias {

        // Índices: mapea id de ciudad → índice de fila/columna en la matriz
        private final Map<String, Integer> indices = new HashMap<>();
        private final String[] ciudadIds;
        private final double[][] distancias;

        private MatrizDistancias(String[] ciudadIds, double[][] distancias) {
            this.ciudadIds = ciudadIds;
            this.distancias = distancias;
            for (int i = 0; i < ciudadIds.length; i++) {
                indices.put(ciudadIds[i], i);
            }
        }

        public double getDistancia(String idOrigen, String idDestino) {
            Integer i = indices.get(idOrigen);
            Integer j = indices.get(idDestino);
            if (i == null || j == null) return Double.MAX_VALUE;
            return distancias[i][j];
        }

        /**
         * De una lista de ciudades candidatas, retorna la que está más cerca
         * del origen según la matriz precalculada.
         * Útil para el algoritmo greedy de entrega más cercana primero.
         */
        public String getCiudadMasCercana(String idOrigen, List<String> candidatos) {
            String masCercana = null;
            double menorDistancia = Double.MAX_VALUE;
            for (String candidato : candidatos) {
                double d = getDistancia(idOrigen, candidato);
                if (d < menorDistancia) {
                    menorDistancia = d;
                    masCercana = candidato;
                }
            }
            return masCercana;
        }

        public double[][] getMatriz() { return distancias; }
        public String[] getIndices() { return ciudadIds; }
    }

    /**
     * Ejecuta Floyd-Warshall sobre el grafo y retorna la matriz de distancias mínimas.
     */
    public static MatrizDistancias calcular(Grafo grafo) {
        List<Ciudad> listaCiudades = new ArrayList<>(grafo.getCiudades());
        int n = listaCiudades.size();
        String[] ids = new String[n];
        Map<String, Integer> indices = new HashMap<>();

        for (int i = 0; i < n; i++) {
            ids[i] = listaCiudades.get(i).getId();
            indices.put(ids[i], i);
        }

        // Inicializar la matriz: 0 en diagonal, infinito en el resto
        double[][] dist = new double[n][n];
        for (double[] fila : dist) Arrays.fill(fila, Double.MAX_VALUE / 2);
        for (int i = 0; i < n; i++) dist[i][i] = 0;

        // Cargar las aristas reales del grafo en la matriz
        for (Ruta r : grafo.getRutas()) {
            Integer i = indices.get(r.getOrigen());
            Integer j = indices.get(r.getDestino());
            if (i != null && j != null) {
                dist[i][j] = Math.min(dist[i][j], r.getDistanciaKm());
                if (r.isEsBidireccional()) {
                    dist[j][i] = Math.min(dist[j][i], r.getDistanciaKm());
                }
            }
        }

        // Núcleo de Floyd-Warshall: intentar mejorar cada par (i,j) usando k como intermediario
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }

        return new MatrizDistancias(ids, dist);
    }
}