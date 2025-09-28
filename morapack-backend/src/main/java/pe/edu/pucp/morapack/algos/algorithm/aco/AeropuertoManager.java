package MoraTravel;
import java.io.*;
import java.util.*;

public class AeropuertoManager {
    private static List<Aeropuerto> aeropuertos = new ArrayList<>();
    private static String continenteActual = "";

    public static void cargarAeropuertos(String rutaArchivo) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo));
        String line;
        int id = 1;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Detectar continente (líneas que no empiezan con dígito)
            if (!Character.isDigit(line.charAt(0))) {
                continenteActual = line.trim();
                continue;
            }

            try {
                // Extraer latitud y longitud por substring
                int latIdx = line.indexOf("Latitude:");
                int longIdx = line.indexOf("Longitude:");
                String latitud = "";
                String longitud = "";
                if (latIdx != -1 && longIdx != -1) {
                    latitud = line.substring(latIdx + 9, longIdx).replaceAll("[^0-9°'\" NSEW-]", "").trim();
                    longitud = line.substring(longIdx + 10).replaceAll("[^0-9°'\" NSEW-]", "").trim();
                }

                // Extraer los primeros campos por split limitado
                String datosPrincipales = (latIdx != -1) ? line.substring(0, latIdx).trim() : line;
                String[] partes = datosPrincipales.split("\\s+");
                if (partes.length < 7) {
                    System.err.println("Error: línea con menos de 7 campos\nLínea: " + line);
                    continue;
                }
                String codigo = partes[1];
                // Reconstruir ciudad (puede tener espacios)
                StringBuilder ciudadBuilder = new StringBuilder();
                int idxCiudad = 2;
                // Buscar el índice del país (el siguiente campo después de ciudad que empieza con mayúscula y no es alias)
                int idxPais = idxCiudad;
                for (int i = idxCiudad; i < partes.length; i++) {
                    if (partes[i].length() > 0 && Character.isUpperCase(partes[i].charAt(0)) && i > idxCiudad) {
                        idxPais = i;
                        break;
                    }
                    ciudadBuilder.append(partes[i]).append(" ");
                }
                String ciudad = ciudadBuilder.toString().trim();
                String pais = partes[idxPais];
                // El alias está después del país
                int idxAlias = idxPais + 1;
                int idxHuso = idxAlias + 1;
                int idxCapacidad = idxAlias + 2;
                int husoHorario = Integer.parseInt(partes[idxHuso]);
                int capacidadAlmacen = Integer.parseInt(partes[idxCapacidad]);

                Aeropuerto aeropuerto = new Aeropuerto(id++, codigo, ciudad, pais, continenteActual, capacidadAlmacen, latitud, longitud, husoHorario);
                aeropuertos.add(aeropuerto);
            } catch (Exception e) {
                System.err.println("Error al cargar aeropuerto: " + e.getMessage() + "\nLínea: " + line);
            }
        }
        reader.close();
    }

    public static List<Aeropuerto> obtenerTodos() {
        return aeropuertos;
    }
}
