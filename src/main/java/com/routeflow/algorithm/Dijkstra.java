package com.routeflow.algorithm;

import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import com.routeflow.model.Ruta;

import java.time.LocalTime;
import java.util.*;

/**
 * Dijkstra ponderado por tiempo real de viaje.
 *
 * El costo de cada arista es getCostoEfectivo(hora): minutos estimados
 * considerando tipo de camino, factor de tráfico y hora del día.
 * Bloqueos se modelan como costo 999_999, lo que los excluye de facto.
 *
 * La cola de prioridad está implementada manualmente con ArrayList.
 */
public class Dijkstra {

    private static class NodoPrioridad {
        final String id;
        final double costo;
        NodoPrioridad(String id, double costo) { this.id = id; this.costo = costo; }
    }

    public static class ResultadoDijkstra {
        public final List<String> camino;
        public final double distanciaKm;       // suma Haversine del camino real
        public final double tiempoEstimadoMin; // costo optimizado por Dijkstra (minutos)
        public final List<String> pasosDescripcion;
        public final boolean encontrado;
        public final int horaUsada;

        public ResultadoDijkstra(List<String> camino, double distanciaKm,
                                  double tiempoEstimadoMin, List<String> pasosDescripcion,
                                  boolean encontrado, int horaUsada) {
            this.camino             = camino;
            this.distanciaKm        = distanciaKm;
            this.tiempoEstimadoMin  = tiempoEstimadoMin;
            this.pasosDescripcion   = pasosDescripcion;
            this.encontrado         = encontrado;
            this.horaUsada          = horaUsada;
        }
    }

    /** Usa la hora del sistema. */
    public static ResultadoDijkstra calcular(Grafo grafo, String origenId, String destinoId) {
        return calcular(grafo, origenId, destinoId, LocalTime.now().getHour());
    }

    /** Calcula la ruta óptima en tiempo para la hora indicada (0-23). */
    public static ResultadoDijkstra calcular(Grafo grafo, String origenId,
                                              String destinoId, int hora) {
        Map<String, Double> costos     = new HashMap<>();
        Map<String, String> predecesor = new HashMap<>();
        List<NodoPrioridad> cola       = new ArrayList<>();

        for (Ciudad c : grafo.getCiudades()) costos.put(c.getId(), Double.MAX_VALUE);
        costos.put(origenId, 0.0);
        cola.add(new NodoPrioridad(origenId, 0.0));

        Set<String> procesados = new HashSet<>();

        while (!cola.isEmpty()) {
            NodoPrioridad actual = extraerMinimo(cola);
            if (procesados.contains(actual.id)) continue;
            procesados.add(actual.id);
            if (actual.id.equals(destinoId)) break;

            for (Ruta vecino : grafo.getVecinos(actual.id)) {
                double costo = vecino.getCostoEfectivo(hora);
                double nuevoCosto = costos.get(actual.id) + costo;
                if (nuevoCosto < costos.getOrDefault(vecino.getDestino(), Double.MAX_VALUE)) {
                    costos.put(vecino.getDestino(), nuevoCosto);
                    predecesor.put(vecino.getDestino(), actual.id);
                    cola.add(new NodoPrioridad(vecino.getDestino(), nuevoCosto));
                }
            }
        }

        if (!predecesor.containsKey(destinoId) && !origenId.equals(destinoId)) {
            return new ResultadoDijkstra(
                    Collections.emptyList(), 0, 0, Collections.emptyList(), false, hora);
        }

        List<String> camino = reconstruirCamino(predecesor, origenId, destinoId);
        double distKm       = sumarDistanciaKm(camino, grafo);
        double tiempoMin    = costos.getOrDefault(destinoId, 0.0);
        List<String> pasos  = generarPasos(camino, grafo, hora);

        return new ResultadoDijkstra(camino, distKm, tiempoMin, pasos, true, hora);
    }

    private static NodoPrioridad extraerMinimo(List<NodoPrioridad> lista) {
        int idx = 0;
        for (int i = 1; i < lista.size(); i++) {
            if (lista.get(i).costo < lista.get(idx).costo) idx = i;
        }
        return lista.remove(idx);
    }

    private static List<String> reconstruirCamino(Map<String, String> pred,
                                                    String origen, String destino) {
        LinkedList<String> camino = new LinkedList<>();
        String actual = destino;
        while (actual != null) {
            camino.addFirst(actual);
            actual = pred.get(actual);
        }
        return camino;
    }

    private static double sumarDistanciaKm(List<String> camino, Grafo grafo) {
        double total = 0;
        for (int i = 0; i < camino.size() - 1; i++) {
            Ciudad a = grafo.getCiudad(camino.get(i));
            Ciudad b = grafo.getCiudad(camino.get(i + 1));
            if (a != null && b != null) {
                total += Haversine.calcularDistanciaKm(
                        a.getLatitud(), a.getLongitud(),
                        b.getLatitud(), b.getLongitud());
            }
        }
        return total;
    }

    private static List<String> generarPasos(List<String> camino, Grafo grafo, int hora) {
        List<String> pasos = new ArrayList<>();
        for (int i = 0; i < camino.size() - 1; i++) {
            Ciudad desde = grafo.getCiudad(camino.get(i));
            Ciudad hasta = grafo.getCiudad(camino.get(i + 1));
            if (desde == null || hasta == null) continue;

            double dist = Haversine.calcularDistanciaKm(
                    desde.getLatitud(), desde.getLongitud(),
                    hasta.getLatitud(), hasta.getLongitud());

            // Buscar la ruta real para obtener su costo efectivo
            double tiempo = 0;
            for (Ruta r : grafo.getVecinos(camino.get(i))) {
                if (r.getDestino().equals(camino.get(i + 1))) {
                    tiempo = r.getCostoEfectivo(hora);
                    break;
                }
            }
            pasos.add(String.format("%s → %s (%.1f km, ~%.0f min)",
                    desde.getNombre(), hasta.getNombre(), dist, tiempo));
        }
        return pasos;
    }
}