package MoraTravel;

import java.io.File;
import java.util.*;

public class App {

    // ===== Auxiliar de validación para líneas de pedidos =====
    // (ajusta el mínimo de columnas según tu formato real de pedidos.csv)
    private static boolean validarPartes(String[] partes) {
        if (partes == null || partes.length < 6) return false;
        for (int i = 0; i < 6; i++) {
            if (partes[i] == null || partes[i].trim().isEmpty()) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        // =========================
        // 1) Cargar AEROPUERTOS
        // =========================
        long inicio = System.currentTimeMillis();

        List<Aeropuerto> aeropuertos = new ArrayList<>();
        String rutaAeropuertos = "C:\\Users\\USUARIO\\Desktop\\DP1\\Lectura\\DP1_TA\\DP1_TA\\src\\MoraTravel\\aeropuertos.csv";
        try (Scanner scanner = new Scanner(new File(rutaAeropuertos))) {
            if (scanner.hasNextLine()) scanner.nextLine(); // Saltar encabezado
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) continue;
                String[] partes = line.split(",");
                if (partes.length < 10) continue;

                int id = Integer.parseInt(partes[0].trim());
                String continente = partes[1].trim();
                String codigo = partes[2].trim();
                String ciudad = partes[3].trim();
                String pais = partes[4].trim();
                // String acronimo = partes[5].trim(); // si no lo usas, ignóralo
                int husoHorario = Integer.parseInt(partes[6].trim());
                int capacidadAlmacen = Integer.parseInt(partes[7].trim());
                String latitud = partes[8].trim();
                String longitud = partes[9].trim();

                // Ajusta el orden a tu constructor real de Aeropuerto
                Aeropuerto aeropuerto = new Aeropuerto(
                        id, codigo, ciudad, pais, continente,
                        capacidadAlmacen, latitud, longitud, husoHorario
                );
                aeropuertos.add(aeropuerto);
            }
        } catch (Exception e) {
            System.err.println("Error leyendo aeropuertos: " + e.getMessage());
        }
        System.out.println("Aeropuertos cargados: " + aeropuertos.size());

        // Índice por código para búsquedas rápidas
        Map<String, Aeropuerto> aeropuertoPorCodigo = new HashMap<>();
        for (Aeropuerto a : aeropuertos) aeropuertoPorCodigo.put(a.codigo, a);

        // =========================
        // 2) Cargar VUELOS
        // =========================
        List<flight> vuelos = new ArrayList<>();
        String rutaVuelos = "C:\\Users\\USUARIO\\Desktop\\DP1\\Lectura\\DP1_TA\\DP1_TA\\src\\MoraTravel\\Planes_vuelo.csv";
        try (Scanner scanner = new Scanner(new File(rutaVuelos))) {
            if (scanner.hasNextLine()) scanner.nextLine(); // Saltar encabezado
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) continue;
                String[] partes = line.split(",");
                if (partes.length < 5) continue;

                String origen = partes[0].trim();
                String destino = partes[1].trim();
                // partes[2], partes[3] si tienes otros campos (p.ej. frecuencia)
                int capacidad = Integer.parseInt(partes[4].trim());

                Aeropuerto ao = aeropuertoPorCodigo.get(origen);
                Aeropuerto ad = aeropuertoPorCodigo.get(destino);
                if (ao == null || ad == null) {
                    System.err.printf("Aeropuerto no encontrado para vuelo %s -> %s%n", origen, destino);
                    continue;
                }

                // id incremental simple; frecuencia 1 por defecto (ajusta si tu CSV la trae)
                flight vuelo = new flight(vuelos.size(), ao, ad, capacidad, 1);
                vuelos.add(vuelo);
            }
        } catch (Exception e) {
            System.err.println("Error leyendo vuelos: " + e.getMessage());
        }
        System.out.println("Vuelos cargados: " + vuelos.size());

    }
}