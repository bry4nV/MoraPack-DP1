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
            int horaSalida = v.horaOrigen.getHour() * 60 + v.horaOrigen.getMinute();
            int horaLlegada = v.horaDestino.getHour() * 60 + v.horaDestino.getMinute();
            int plazoMax = v.origen.continente.equals(v.destino.continente) ? 12*60 : 24*60;

            int duracionVuelo = (horaLlegada - horaSalida + 24*60) % (24*60);
            if (duracionVuelo <= plazoMax) {
                grafo.agregarArista(v.origen, v.destino, v.capacidadMax, horaSalida, horaLlegada);
            } else {
                System.out.printf("Vuelo %s -> %s excede plazo máximo (%d min) y no se agrega%n",
                        v.origen.codigo, v.destino.codigo, plazoMax);
            }
        }





        // Definir sedes principales
        List<Aeropuerto> sedes = new ArrayList<>();
        sedes.add(aeropuertoPorCodigo.get("SPIM")); // Lima
        sedes.add(aeropuertoPorCodigo.get("EBCI")); // Bruselas
        sedes.add(aeropuertoPorCodigo.get("UBBB")); // Baku

        
        Random random = new Random(); 
        // Inicializar ACO
        ACOPedidos ac = new ACOPedidos(
            grafo,
            sedes,
            5,    // número de hormigas
            50,    // número de iteraciones
            0.1,   // tasa de evaporación
            100.0,  // incremento de feromona
            random // semilla fija para reproducibilidad
        );

        double tiempoTotalEntrega=0.0;
        int totalPedidos=pedidos.size();
        // Ejecutar ACO sobre los pedidos
        long startTime = System.nanoTime();
        Map<Pedido, Hormiga> rutasOptimas = ac.solucionar(pedidos, aeropuertoPorCodigo);
        long endTime = System.nanoTime(); 
         // Calcular el tiempo total de ejecución en milisegundos
        long duration = (endTime - startTime) / 1000000; 



        System.out.println("\n--- Resumen de rutas optimas ---");
         for (Pedido p : rutasOptimas.keySet()) {
            Hormiga h = rutasOptimas.get(p);
            if (h != null && !h.ruta.isEmpty()) {
                System.out.print("Pedido " + p.getIdCliente() + ": ");
                for (Arista a : h.ruta) {
                    System.out.print(a.origen.codigo + "->" + a.destino.codigo + " ");
                }
                System.out.println("| Tiempo total: " + h.tiempoTotal + "min");
                tiempoTotalEntrega += h.tiempoTotal;
            } else {
                System.out.println("Pedido " + p.getIdCliente() + ": No se pudo generar ruta");
            }
         }


        

        // Mostrar el tiempo total de ejecución
        System.out.println("\nTiempo total de ejecución: " + duration + " ms");
        double tiempoPromedioEntrega = tiempoTotalEntrega / totalPedidos;

        // Mostrar el tiempo promedio de entrega en minutos
        System.out.println("\nTiempo promedio de entrega de los pedidos: " + tiempoPromedioEntrega + " minutos");


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
                String horaOrigenStr = partes[2].trim(); // HH:mm
                String horaDestinoStr = partes[3].trim(); // HH:mm
                int capacidad = Integer.parseInt(partes[4].trim());

                Aeropuerto ao = aeropuertoPorCodigo.get(origen);
                Aeropuerto ad = aeropuertoPorCodigo.get(destino);
                if (ao == null || ad == null) {
                    System.err.printf("Aeropuerto no encontrado para vuelo %s -> %s%n", origen, destino);
                    continue;
                }

                // id incremental simple; frecuencia 1 por defecto
                Vuelo vuelo = new Vuelo(vuelos.size(), ao, ad, horaOrigenStr, horaDestinoStr, capacidad, 1);
                vuelos.add(vuelo);
            }
        } catch (Exception e) {
            System.err.println("Error leyendo vuelos: " + e.getMessage());
        }
        System.out.println("Vuelos cargados: " + vuelos.size());
        return vuelos;
    }
}
