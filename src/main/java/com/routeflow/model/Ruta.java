package com.routeflow.model;

/**
 * Representa una arista del grafo: la conexión entre dos ciudades.
 *
 * La distanciaKm NO se recibe del usuario; la calcula Haversine
 * a partir de las coordenadas reales de las ciudades.
 * esBidireccional=true significa que se puede transitar en ambas direcciones.
 */
public class Ruta {

    private String origen;
    private String destino;
    private double distanciaKm;
    private boolean esBidireccional;

    public Ruta(String origen, String destino, double distanciaKm, boolean esBidireccional) {
        this.origen = origen;
        this.destino = destino;
        this.distanciaKm = distanciaKm;
        this.esBidireccional = esBidireccional;
    }

    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }
    public double getDistanciaKm() { return distanciaKm; }
    public boolean isEsBidireccional() { return esBidireccional; }

    @Override
    public String toString() {
        String direccion = esBidireccional ? "<->" : "->";
        return String.format("Ruta{%s %s %s, %.2f km}", origen, direccion, destino, distanciaKm);
    }
}