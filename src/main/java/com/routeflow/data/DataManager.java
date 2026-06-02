package com.routeflow.data;

import com.routeflow.model.Ciudad;
import com.routeflow.model.Grafo;
import com.routeflow.model.Ruta;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia del grafo en texto plano.
 *
 * Formato CIUDAD: CIUDAD|id|nombre|lat|lon|esBodega
 * Formato RUTA:   RUTA|id1|id2|bidi|factorTrafico|tipoCamino
 * Retrocompatible: si la RUTA solo tiene 4 campos usa defaults (1.0, CARRETERA).
 */
public class DataManager {

    public static Grafo cargarDesdeArchivo(String rutaArchivo) throws IOException {
        Grafo grafo = new Grafo();
        List<String[]> rutasPendientes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            String linea;
            int nLinea = 0;
            while ((linea = reader.readLine()) != null) {
                nLinea++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] p = linea.split("\\|");

                if (p[0].equals("CIUDAD")) {
                    if (p.length < 6) { warn(nLinea, linea); continue; }
                    try {
                        grafo.agregarCiudad(new Ciudad(
                                p[1].trim(), p[2].trim(),
                                Double.parseDouble(p[3].trim()),
                                Double.parseDouble(p[4].trim()),
                                Boolean.parseBoolean(p[5].trim())));
                    } catch (NumberFormatException e) {
                        warn(nLinea, linea);
                    }

                } else if (p[0].equals("RUTA")) {
                    if (p.length < 4) { warn(nLinea, linea); continue; }
                    // Guardar todos los campos disponibles
                    rutasPendientes.add(p);
                }
            }
        }

        for (String[] p : rutasPendientes) {
            try {
                String origen  = p[1].trim();
                String destino = p[2].trim();
                boolean bidi   = Boolean.parseBoolean(p[3].trim());

                if (p.length >= 6) {
                    double factor          = Double.parseDouble(p[4].trim());
                    Ruta.TipoCamino tipo   = Ruta.TipoCamino.valueOf(p[5].trim());
                    grafo.agregarRuta(origen, destino, bidi, factor, tipo);
                } else {
                    grafo.agregarRuta(origen, destino, bidi);
                }
            } catch (Exception e) {
                System.err.println("Error al agregar ruta " + p[1] + "->" + p[2] + ": " + e.getMessage());
            }
        }

        return grafo;
    }

    public static void guardarEnArchivo(Grafo grafo, String rutaArchivo) throws IOException {
        try (PrintWriter w = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            w.println("# RouteFlow GPS - Mapa exportado");
            w.println("# Formato CIUDAD: CIUDAD|id|nombre|lat|lon|esBodega");
            w.println("# Formato RUTA:   RUTA|id1|id2|bidi|factorTrafico|tipoCamino");
            w.println();

            for (Ciudad c : grafo.getCiudades()) {
                w.printf("CIUDAD|%s|%s|%.6f|%.6f|%b%n",
                        c.getId(), c.getNombre(),
                        c.getLatitud(), c.getLongitud(), c.isEsBodega());
            }
            w.println();

            for (Ruta r : grafo.getRutas()) {
                w.printf("RUTA|%s|%s|%b|%.2f|%s%n",
                        r.getOrigen(), r.getDestino(), r.isEsBidireccional(),
                        r.getFactorTrafico(), r.getTipoCamino().name());
            }
        }
    }

    private static void warn(int linea, String contenido) {
        System.err.println("Línea " + linea + " malformada: " + contenido);
    }
}