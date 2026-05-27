package com.routeflow.model;

import com.routeflow.algorithm.Haversine;

import java.util.*;

/**
 * Grafo dirigido (o bidireccional) implementado con listas de adyacencia.
 *
 * Usa un HashMap para mapear cada id de ciudad a su lista de rutas salientes.
 * Las distancias de las aristas se calculan automáticamente con la fórmula
 * de Haversine usando las coordenadas GPS reales de las ciudades.
 */
public class Grafo {

    // Almacena las ciudades (nodos) indexadas por su id único
    private final Map<String, Ciudad> ciudades = new HashMap<>();

    // Lista de adyacencia: cada id mapea a las rutas que salen de esa ciudad
    private final Map<String, List<Ruta>> adyacencia = new HashMap<>();

    public void agregarCiudad(Ciudad ciudad) {
        ciudades.put(ciudad.getId(), ciudad);
        adyacencia.putIfAbsent(ciudad.getId(), new ArrayList<>());
    }

    /**
     * Agrega una ruta entre dos ciudades ya existentes en el grafo.
     * La distancia se calcula con Haversine, no se recibe como parámetro.
     */
    public void agregarRuta(String origenId, String destinoId, boolean bidireccional) {
        Ciudad origen = ciudades.get(origenId);
        Ciudad destino = ciudades.get(destinoId);
        if (origen == null || destino == null) {
            throw new IllegalArgumentException(
                "Ciudad no encontrada: " + (origen == null ? origenId : destinoId));
        }

        double distancia = Haversine.calcularDistanciaKm(
                origen.getLatitud(), origen.getLongitud(),
                destino.getLatitud(), destino.getLongitud());

        adyacencia.get(origenId).add(new Ruta(origenId, destinoId, distancia, bidireccional));

        if (bidireccional) {
            adyacencia.get(destinoId).add(new Ruta(destinoId, origenId, distancia, bidireccional));
        }
    }

    public void eliminarCiudad(String id) {
        ciudades.remove(id);
        adyacencia.remove(id);
        // Eliminar todas las rutas que llegaban a esta ciudad
        for (List<Ruta> rutas : adyacencia.values()) {
            rutas.removeIf(r -> r.getDestino().equals(id));
        }
    }

    public void eliminarRuta(String origenId, String destinoId) {
        List<Ruta> rutas = adyacencia.get(origenId);
        if (rutas != null) {
            rutas.removeIf(r -> r.getDestino().equals(destinoId));
        }
    }

    public Ciudad getCiudad(String id) {
        return ciudades.get(id);
    }

    public List<Ruta> getVecinos(String id) {
        return adyacencia.getOrDefault(id, Collections.emptyList());
    }

    public Collection<Ciudad> getCiudades() {
        return ciudades.values();
    }

    /**
     * Retorna todas las aristas del grafo (sin duplicados en bidireccionales).
     */
    public List<Ruta> getRutas() {
        Set<String> vistas = new HashSet<>();
        List<Ruta> todas = new ArrayList<>();
        for (List<Ruta> lista : adyacencia.values()) {
            for (Ruta r : lista) {
                String clave = r.getOrigen().compareTo(r.getDestino()) < 0
                        ? r.getOrigen() + "-" + r.getDestino()
                        : r.getDestino() + "-" + r.getOrigen();
                if (vistas.add(clave)) {
                    todas.add(r);
                }
            }
        }
        return todas;
    }

    public int getNodosCount() {
        return ciudades.size();
    }

    public int getAristasCount() {
        return getRutas().size();
    }

    /**
     * BFS interno para verificar si existe algún camino entre dos nodos.
     * Útil antes de lanzar Dijkstra en grafos potencialmente desconectados.
     */
    public boolean existeCamino(String origenId, String destinoId) {
        if (!ciudades.containsKey(origenId) || !ciudades.containsKey(destinoId)) return false;
        if (origenId.equals(destinoId)) return true;

        Set<String> visitados = new HashSet<>();
        Queue<String> cola = new LinkedList<>();
        cola.add(origenId);
        visitados.add(origenId);

        while (!cola.isEmpty()) {
            String actual = cola.poll();
            for (Ruta r : getVecinos(actual)) {
                if (r.getDestino().equals(destinoId)) return true;
                if (visitados.add(r.getDestino())) {
                    cola.add(r.getDestino());
                }
            }
        }
        return false;
    }
}