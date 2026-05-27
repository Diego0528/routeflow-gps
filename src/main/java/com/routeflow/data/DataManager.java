package com.routeflow.data;

import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import com.routeflow.model.Ruta;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee y escribe el grafo desde/hacia archivos de texto plano.
 *
 * El formato de archivo es simple e intencional: una línea por entidad,
 * con campos separados por "|". Esto facilita editarlo a mano o generarlo
 * con scripts externos.
 *
 * Las distancias NO se guardan en el archivo porque se recalculan con
 * Haversine cada vez que se carga. Esto asegura consistencia con los
 * datos GPS reales.
 */
public class DataManager {

    /**
     * Carga el grafo completo desde un archivo de texto.
     * Las líneas que empiezan con "#" son comentarios y se ignoran.
     */
    public static Grafo cargarDesdeArchivo(String rutaArchivo) throws IOException {
        Grafo grafo = new Grafo();
        // Guardamos las rutas para procesarlas después de cargar todas las ciudades
        List<String[]> rutasPendientes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            String linea;
            int numeroLinea = 0;

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] partes = linea.split("\\|");

                if (partes[0].equals("CIUDAD")) {
                    if (partes.length < 6) {
                        System.err.println("Línea " + numeroLinea + " malformada (CIUDAD): " + linea);
                        continue;
                    }
                    try {
                        String id = partes[1].trim();
                        String nombre = partes[2].trim();
                        double lat = Double.parseDouble(partes[3].trim());
                        double lon = Double.parseDouble(partes[4].trim());
                        boolean esBodega = Boolean.parseBoolean(partes[5].trim());
                        grafo.agregarCiudad(new Ciudad(id, nombre, lat, lon, esBodega));
                    } catch (NumberFormatException e) {
                        System.err.println("Error al parsear coordenadas en línea " + numeroLinea + ": " + e.getMessage());
                    }

                } else if (partes[0].equals("RUTA")) {
                    if (partes.length < 4) {
                        System.err.println("Línea " + numeroLinea + " malformada (RUTA): " + linea);
                        continue;
                    }
                    // Guardamos para procesar después de que todas las ciudades estén cargadas
                    rutasPendientes.add(new String[]{
                        partes[1].trim(), partes[2].trim(), partes[3].trim()
                    });
                }
            }
        }

        // Ahora sí podemos agregar las rutas porque todas las ciudades ya existen
        for (String[] r : rutasPendientes) {
            try {
                boolean bidireccional = Boolean.parseBoolean(r[2]);
                grafo.agregarRuta(r[0], r[1], bidireccional);
            } catch (IllegalArgumentException e) {
                System.err.println("Error al agregar ruta " + r[0] + "->" + r[1] + ": " + e.getMessage());
            }
        }

        return grafo;
    }

    /**
     * Guarda el grafo actual en un archivo de texto con el mismo formato
     * que puede volver a leerse con cargarDesdeArchivo().
     */
    public static void guardarEnArchivo(Grafo grafo, String rutaArchivo) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            writer.println("# RouteFlow GPS - Mapa exportado");
            writer.println("# Formato: CIUDAD|id|nombre|latitud|longitud|esBodega");
            writer.println("# Formato: RUTA|idOrigen|idDestino|bidireccional");
            writer.println();

            for (Ciudad c : grafo.getCiudades()) {
                writer.printf("CIUDAD|%s|%s|%.6f|%.6f|%b%n",
                        c.getId(), c.getNombre(),
                        c.getLatitud(), c.getLongitud(),
                        c.isEsBodega());
            }

            writer.println();

            for (Ruta r : grafo.getRutas()) {
                writer.printf("RUTA|%s|%s|%b%n",
                        r.getOrigen(), r.getDestino(), r.isEsBidireccional());
            }
        }
    }
}