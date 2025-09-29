package pe.edu.pucp.morapack.algos.algorithm.aco;
import java.io.File;
import java.util.*;



public class App {
    public static void main(String[] args) {
        // Cargar aeropuertos
        String rutaAeropuertos = "C:\\Users\\USUARIO\\Desktop\\DP1\\github\\MoraPack-DP1\\morapack-backend\\src\\main\\java\\pe\\edu\\pucp\\morapack\\algos\\algorithm\\aco\\aeropuertos.csv";
        List<Aeropuerto> aeropuertos = cargarAeropuertos(rutaAeropuertos);

        // Índice por código para búsquedas rápidas
        Map<String, Aeropuerto> aeropuertoPorCodigo = new HashMap<>();
        for (Aeropuerto a : aeropuertos) 
            aeropuertoPorCodigo.put(a.codigo, a);
        
        // Cargar vuelos
        String rutaVuelos = "C:\\Users\\USUARIO\\Desktop\\DP1\\github\\MoraPack-DP1\\morapack-backend\\src\\main\\java\\pe\\edu\\pucp\\morapack\\algos\\algorithm\\aco\\vuelos.csv";
        List<Vuelo> vuelos = cargarVuelos(rutaVuelos, aeropuertoPorCodigo);


        //Cargar Pedidos
        String rutaPedidos = "C:\\Users\\USUARIO\\Desktop\\DP1\\github\\MoraPack-DP1\\morapack-backend\\src\\main\\java\\pe\\edu\\pucp\\morapack\\algos\\algorithm\\aco\\pedidos.csv";
        List<Pedido> pedidos = cargarPedidos(rutaPedidos);

        //Representación del Grafo (Aeropuertos y Vuelos)
        //Aeropuertos -> Nodos
        //Vuelos -> Aristas
        Grafo grafo = new Grafo();

        // Agregar aeropuertos como nodos
        for (Aeropuerto a : aeropuertos) {
            grafo.agregarAeropuerto(a);
        }

        // Agregar vuelos como aristas
        for (Vuelo v : vuelos) {
            double tiempoVuelo = v.origen.continente.equals(v.destino.continente) ? 12 : 24; // ejemplo: 12 horas dentro de continente, 24 entre continentes
            grafo.agregarArista(v.origen, v.destino,v.capacidadMax,tiempoVuelo);
        }





        // Definir sedes principales
        List<Aeropuerto> sedes = new ArrayList<>();
        sedes.add(aeropuertoPorCodigo.get("SPIM")); // Lima
        sedes.add(aeropuertoPorCodigo.get("EBCI")); // Bruselas
        sedes.add(aeropuertoPorCodigo.get("UBBB")); // Baku

        
        
        // Inicializar ACO
        ACOPedidos ac = new ACOPedidos(
            grafo,
            sedes,
            10,    // número de hormigas
            50,    // número de iteraciones
            0.5,   // tasa de evaporación
            100.0  // incremento de feromona
        );







       /* for (Aeropuerto a : grafo.adyacencias.keySet()) {
            System.out.print(a.codigo + " -> ");
            for (Arista ar : grafo.adyacencias.get(a)) {
                 System.out.print(ar.destino.codigo + "(" + ar.tiempo + "h) ");
         }
        System.out.println();
        }
        */






        // Ejecutar ACO sobre los pedidos
        Map<Pedido, Hormiga> rutasOptimas = ac.solucionar(pedidos, aeropuertoPorCodigo);
        System.out.println("\n--- Resumen de rutas optimas ---");
         for (Pedido p : rutasOptimas.keySet()) {
            Hormiga h = rutasOptimas.get(p);
            if (h != null && !h.ruta.isEmpty()) {
                System.out.print("Pedido " + p.getIdCliente() + ": ");
                for (Arista a : h.ruta) {
                    System.out.print(a.origen.codigo + "->" + a.destino.codigo + " ");
                }
                System.out.println("| Tiempo total: " + h.tiempoTotal + "h");
            } else {
                System.out.println("Pedido " + p.getIdCliente() + ": No se pudo generar ruta");
            }
        }



    }

    public static List<Pedido> cargarPedidos(String rutaArchivo) {
    List<Pedido> pedidos = new ArrayList<>();
    try (Scanner scanner = new Scanner(new File(rutaArchivo))) {
        // Ignorar la primera línea (encabezado)
        if (scanner.hasNextLine()) scanner.nextLine();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue;  // Saltar líneas vacías
            String[] partes = line.split(",");
            if (partes.length < 6) continue;  // Si no tiene el número correcto de columnas, saltar

            try {
                int dia = Integer.parseInt(partes[0].trim());
                int hora = Integer.parseInt(partes[1].trim());
                int minuto = Integer.parseInt(partes[2].trim());
                String destino = partes[3].trim();
                int cantidadPaquetes = Integer.parseInt(partes[4].trim());
                int idCliente = Integer.parseInt(partes[5].trim());

                // Crear el objeto Pedido
                Pedido pedido = new Pedido(dia, hora, minuto, destino, cantidadPaquetes, idCliente);
                pedidos.add(pedido);
            } catch (NumberFormatException e) {
                System.err.println("Error al parsear los datos del pedido: " + line);
            }
        }
    } catch (Exception e) {
        System.err.println("Error leyendo pedidos: " + e.getMessage());
    }
    System.out.println("Pedidos cargados: " + pedidos.size());
    return pedidos;
}



    // Método para cargar los aeropuertos
    private static List<Aeropuerto> cargarAeropuertos(String rutaArchivo) {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(rutaArchivo))) {
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
                int husoHorario = Integer.parseInt(partes[6].trim());
                int capacidadAlmacen = Integer.parseInt(partes[7].trim());
                String latitud = partes[8].trim();
                String longitud = partes[9].trim();

                // Ajusta el constructor real de Aeropuerto
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
        return aeropuertos;
    }

    // Método para cargar los vuelos
    private static List<Vuelo> cargarVuelos(String rutaArchivo, Map<String, Aeropuerto> aeropuertoPorCodigo) {
        List<Vuelo> vuelos = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(rutaArchivo))) {
            if (scanner.hasNextLine()) scanner.nextLine(); // Saltar encabezado
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) continue;
                String[] partes = line.split(",");
                if (partes.length < 5) continue;

                String origen = partes[0].trim();
                String destino = partes[1].trim();
                int capacidad = Integer.parseInt(partes[4].trim());

                Aeropuerto ao = aeropuertoPorCodigo.get(origen);
                Aeropuerto ad = aeropuertoPorCodigo.get(destino);
                if (ao == null || ad == null) {
                    System.err.printf("Aeropuerto no encontrado para vuelo %s -> %s%n", origen, destino);
                    continue;
                }

                // id incremental simple; frecuencia 1 por defecto
                Vuelo vuelo = new Vuelo(vuelos.size(), ao, ad, capacidad, 1);
                vuelos.add(vuelo);
            }
        } catch (Exception e) {
            System.err.println("Error leyendo vuelos: " + e.getMessage());
        }
        System.out.println("Vuelos cargados: " + vuelos.size());
        return vuelos;
    }
}
