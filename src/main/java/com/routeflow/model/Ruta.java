package com.routeflow.model;

/**
 * Arista del grafo con soporte de ponderación dinámica.
 *
 * El costo efectivo no es solo distancia: considera tipo de camino
 * (velocidad base), factor de tráfico manual y hora del día.
 * Un road bloqueado retorna costo 999_999, que Dijkstra trata como infinito.
 */
public class Ruta {

    public enum TipoCamino {
        CARRETERA(70.0),
        URBANO(35.0),
        MONTAÑA(25.0),
        RESIDENCIAL(20.0);

        public final double velocidadKmH;
        TipoCamino(double v) { this.velocidadKmH = v; }
    }

    private final String origen;
    private final String destino;
    private final double distanciaKm;
    private final boolean esBidireccional;
    private double factorTrafico;
    private boolean bloqueado;
    private TipoCamino tipoCamino;

    public Ruta(String origen, String destino, double distanciaKm, boolean esBidireccional) {
        this(origen, destino, distanciaKm, esBidireccional, 1.0, TipoCamino.CARRETERA);
    }

    public Ruta(String origen, String destino, double distanciaKm, boolean esBidireccional,
                double factorTrafico, TipoCamino tipoCamino) {
        this.origen = origen;
        this.destino = destino;
        this.distanciaKm = distanciaKm;
        this.esBidireccional = esBidireccional;
        this.factorTrafico = factorTrafico;
        this.tipoCamino = (tipoCamino != null) ? tipoCamino : TipoCamino.CARRETERA;
        this.bloqueado = false;
    }

    /**
     * Costo en minutos considerando tipo de camino, tráfico manual y hora del día.
     * Dijkstra minimiza este valor, no la distancia en km.
     */
    public double getCostoEfectivo(int hora) {
        if (bloqueado) return 999_999.0;
        double factorHora = factorPorHora(hora);
        return (distanciaKm / tipoCamino.velocidadKmH) * 60.0 * factorTrafico * factorHora;
    }

    /**
     * Multiplica el tiempo base según congestión horaria típica de Antigua.
     */
    public static double factorPorHora(int hora) {
        if (hora >= 6  && hora < 9)  return 2.5;  // rush mañana
        if (hora >= 9  && hora < 12) return 1.2;
        if (hora >= 12 && hora < 14) return 1.8;  // hora almuerzo
        if (hora >= 14 && hora < 17) return 1.0;  // libre
        if (hora >= 17 && hora < 19) return 2.5;  // rush tarde
        if (hora >= 19 && hora < 22) return 1.3;
        return 0.8;                                // madrugada
    }

    public String getOrigen()           { return origen; }
    public String getDestino()          { return destino; }
    public double getDistanciaKm()      { return distanciaKm; }
    public boolean isEsBidireccional()  { return esBidireccional; }
    public double getFactorTrafico()    { return factorTrafico; }
    public boolean isBloqueado()        { return bloqueado; }
    public TipoCamino getTipoCamino()   { return tipoCamino; }

    public void setFactorTrafico(double factorTrafico) { this.factorTrafico = Math.max(0.5, factorTrafico); }
    public void setBloqueado(boolean bloqueado)         { this.bloqueado = bloqueado; }

    @Override
    public String toString() {
        String dir    = esBidireccional ? "<->" : "->";
        String estado = bloqueado ? " [BLOQUEADA]" : "";
        return String.format("Ruta{%s %s %s, %.2f km, x%.1f, %s%s}",
                origen, dir, destino, distanciaKm, factorTrafico, tipoCamino, estado);
    }
}