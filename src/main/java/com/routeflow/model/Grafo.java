package com.routeflow.model;

import com.routeflow.algorithm.Haversine;

import java.util.*;
import java.util.function.Consumer;

public class Grafo {

    private final Map<String, Ciudad> ciudades = new HashMap<>();
    private final Map<String, List<Ruta>> adyacencia = new HashMap<>();

    public void agregarCiudad(Ciudad ciudad) {
        ciudades.put(ciudad.getId(), ciudad);
        adyacencia.putIfAbsent(ciudad.getId(), new ArrayList<>());
    }

    public void agregarRuta(String origenId, String destinoId, boolean bidireccional) {
        agregarRuta(origenId, destinoId, bidireccional, 1.0, Ruta.TipoCamino.CARRETERA);
    }

    public void agregarRuta(String origenId, String destinoId, boolean bidireccional,
                            double factorTrafico, Ruta.TipoCamino tipoCamino) {
        Ciudad origen  = ciudades.get(origenId);
        Ciudad destino = ciudades.get(destinoId);
        if (origen == null || destino == null) {
            throw new IllegalArgumentException(
                "Ciudad no encontrada: " + (origen == null ? origenId : destinoId));
        }
        double distancia = Haversine.calcularDistanciaKm(
                origen.getLatitud(), origen.getLongitud(),
                destino.getLatitud(), destino.getLongitud());

        adyacencia.get(origenId).add(
                new Ruta(origenId, destinoId, distancia, bidireccional, factorTrafico, tipoCamino));

        if (bidireccional) {
            adyacencia.get(destinoId).add(
                    new Ruta(destinoId, origenId, distancia, bidireccional, factorTrafico, tipoCamino));
        }
    }

    /** Bloquea o desbloquea una ruta (en ambas direcciones si es bidireccional). */
    public void setRutaBloqueada(String origenId, String destinoId, boolean bloqueado) {
        aplicarEnRuta(origenId, destinoId, r -> r.setBloqueado(bloqueado));
        aplicarEnRuta(destinoId, origenId, r -> r.setBloqueado(bloqueado));
    }

    /** Aplica un factor de tráfico a una ruta (en ambas direcciones). */
    public void setFactorTrafico(String origenId, String destinoId, double factor) {
        aplicarEnRuta(origenId, destinoId, r -> r.setFactorTrafico(factor));
        aplicarEnRuta(destinoId, origenId, r -> r.setFactorTrafico(factor));
    }

    private void aplicarEnRuta(String origen, String destino, Consumer<Ruta> accion) {
        List<Ruta> rutas = adyacencia.get(origen);
        if (rutas != null) {
            rutas.stream().filter(r -> r.getDestino().equals(destino)).forEach(accion);
        }
    }

    public void eliminarCiudad(String id) {
        ciudades.remove(id);
        adyacencia.remove(id);
        for (List<Ruta> rutas : adyacencia.values()) {
            rutas.removeIf(r -> r.getDestino().equals(id));
        }
    }

    public void eliminarRuta(String origenId, String destinoId) {
        List<Ruta> rutas = adyacencia.get(origenId);
        if (rutas != null) rutas.removeIf(r -> r.getDestino().equals(destinoId));
    }

    public Ciudad       getCiudad(String id)  { return ciudades.get(id); }
    public List<Ruta>   getVecinos(String id) { return adyacencia.getOrDefault(id, Collections.emptyList()); }
    public Collection<Ciudad> getCiudades()   { return ciudades.values(); }
    public int getNodosCount()                { return ciudades.size(); }
    public int getAristasCount()              { return getRutas().size(); }

    public List<Ruta> getRutas() {
        Set<String> vistas = new HashSet<>();
        List<Ruta> todas = new ArrayList<>();
        for (List<Ruta> lista : adyacencia.values()) {
            for (Ruta r : lista) {
                String clave = r.getOrigen().compareTo(r.getDestino()) < 0
                        ? r.getOrigen() + "-" + r.getDestino()
                        : r.getDestino() + "-" + r.getOrigen();
                if (vistas.add(clave)) todas.add(r);
            }
        }
        return todas;
    }

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
                if (visitados.add(r.getDestino())) cola.add(r.getDestino());
            }
        }
        return false;
    }
}