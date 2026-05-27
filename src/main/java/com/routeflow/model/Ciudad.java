package com.routeflow.model;

/**
 * Representa un nodo del grafo: una ciudad o punto de interés
 * en el mapa de Antigua Guatemala.
 *
 * esBodega=true significa que es el punto de origen de los pedidos
 * (almacén desde donde salen las entregas).
 */
public class Ciudad {

    private String id;
    private String nombre;
    private double latitud;
    private double longitud;
    private boolean esBodega;

    public Ciudad(String id, String nombre, double latitud, double longitud, boolean esBodega) {
        this.id = id;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
        this.esBodega = esBodega;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public boolean isEsBodega() { return esBodega; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setLatitud(double latitud) { this.latitud = latitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
    public void setEsBodega(boolean esBodega) { this.esBodega = esBodega; }

    @Override
    public String toString() {
        return String.format("Ciudad{id='%s', nombre='%s', lat=%.4f, lon=%.4f, bodega=%b}",
                id, nombre, latitud, longitud, esBodega);
    }
}