package com.routeflow.algorithm;

/**
 * Implementación de la fórmula de Haversine para calcular distancias reales
 * entre coordenadas geográficas (latitud/longitud en grados decimales).
 *
 * Esta fórmula tiene en cuenta la curvatura de la Tierra, lo que la hace
 * mucho más precisa que la distancia euclidiana para puntos sobre el globo.
 */
public class Haversine {

    // Radio medio de la Tierra en kilómetros (valor estándar WGS84 aproximado)
    private static final double RADIO_TIERRA_KM = 6371.0;

    /**
     * Calcula la distancia en kilómetros entre dos puntos GPS usando Haversine.
     */
    public static double calcularDistanciaKm(double lat1, double lon1,
                                              double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RADIO_TIERRA_KM * c;
    }

    /**
     * Convierte distancia y velocidad a tiempo estimado en minutos.
     */
    public static double calcularTiempoMin(double distanciaKm, double velocidadKmH) {
        if (velocidadKmH <= 0) throw new IllegalArgumentException("La velocidad debe ser positiva");
        return (distanciaKm / velocidadKmH) * 60.0;
    }
}