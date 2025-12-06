package pe.edu.pucp.morapack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.model.Continent;
import pe.edu.pucp.morapack.model.Country;
import pe.edu.pucp.morapack.repository.daily.OrderRepository;
import pe.edu.pucp.morapack.algos.algorithm.IOptimizer;
import pe.edu.pucp.morapack.algos.entities.*; 

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TabuSimulationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private IOptimizer tabuPlanner;

    // Solo 3 Sedes
    private static final Map<String, PlannerAirport> AIRPORT_DB = new HashMap<>();
    private static final Set<String> MAIN_HUBS = new HashSet<>(Arrays.asList("LIM", "EBCI", "UBBB"));

    static {
        Country peru = new Country(1, "Peru", Continent.AMERICA);
        Country azerbaijan = new Country(3, "Azerbaijan", Continent.ASIA);
        Country belgium = new Country(6, "Belgium", Continent.EUROPE);
        Country spain = new Country(2, "Spain", Continent.EUROPE);
        Country usa = new Country(4, "United States", Continent.AMERICA);

        AIRPORT_DB.put("LIM", new PlannerAirport(1, "LIM", "Jorge Chavez", "Lima", peru, 5000, -5, -12.024, -77.112));
        AIRPORT_DB.put("UBBB", new PlannerAirport(3, "UBBB", "Heydar Aliyev", "Baku", azerbaijan, 5000, 4, 40.467, 50.050));
        AIRPORT_DB.put("EBCI", new PlannerAirport(14, "EBCI", "Brussels Airport", "Bruselas", belgium, 440, 2, 50.4592, 4.4536));
        
        // Destinos extra para pruebas
        AIRPORT_DB.put("MAD", new PlannerAirport(2, "MAD", "Barajas", "Madrid", spain, 5000, 1, 40.471, -3.562));
        AIRPORT_DB.put("MIA", new PlannerAirport(4, "MIA", "Miami Intl", "Miami", usa, 5000, -5, 25.793, -80.290));
    }

    @Async 
    public void iniciarSimulacionVuelo(Order dbOrder) {
        try {
            System.out.println("🤖 [TABU] Procesando pedido #" + dbOrder.getOrderNumber());

            // 1. Preparar Destino
            String destCode = dbOrder.getAirportDestinationCode();
            PlannerAirport destAirport = AIRPORT_DB.get(destCode);
            if (destAirport == null) {
                // Si no existe, crear dummy en Paris para no romper
                Country temp = new Country(99, "Temp", Continent.EUROPE);
                destAirport = new PlannerAirport(999, destCode, "Destino " + destCode, "City", temp, 1000, 0, 48.8566, 2.3522);
            }

            List<PlannerAirport> allAirports = new ArrayList<>(AIRPORT_DB.values());
            if (!AIRPORT_DB.containsKey(destCode)) allAirports.add(destAirport);

            // 2. Generar vuelos posibles (SOLO DESDE HUBS)
            List<PlannerFlight> availableFlights = generarVuelosDesdeHubs(destAirport);

            // 3. Crear Orden para el algoritmo (Origen default: LIM)
            PlannerAirport defaultOrigin = AIRPORT_DB.get("LIM");
            PlannerOrder plannerOrder = new PlannerOrder(
                dbOrder.getId().intValue(), dbOrder.getQuantity(), defaultOrigin, destAirport
            );
            
            // 4. Ejecutar Algoritmo
            Solution solution = tabuPlanner.optimize(
                Collections.singletonList(plannerOrder), availableFlights, allAirports
            );

            // 5. Obtener Ruta o Usar Fallback
            List<PlannerFlight> rutaFinal = new ArrayList<>();
            
            PlannerOrder solvedOrder = solution.getAllOrders().stream()
                .filter(o -> o.getId() == plannerOrder.getId()).findFirst().orElse(null);

            if (solvedOrder != null && !solvedOrder.getShipments().isEmpty()) {
                // ✅ IMPORTANTE: Usamos getFlights() (método de la entidad PlannerShipment)
                rutaFinal = solvedOrder.getShipments().get(0).getFlights();
            }

            // 🔥 FALLBACK: Si el algoritmo falló, crear vuelo directo de emergencia
            if (rutaFinal == null || rutaFinal.isEmpty()) {
                System.out.println("⚠️ [TABU] Sin ruta. Usando VUELO DE EMERGENCIA DIRECTO.");
                rutaFinal.add(new PlannerFlight(
                    "EMERGENCY", defaultOrigin, destAirport, LocalDateTime.now(), LocalDateTime.now().plusHours(5), 1000
                ));
            }

            // 6. ANIMAR INMEDIATAMENTE
            animarRutaCompleta(dbOrder, rutaFinal);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<PlannerFlight> generarVuelosDesdeHubs(PlannerAirport dest) {
        List<PlannerFlight> flights = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (String hubCode : MAIN_HUBS) {
            PlannerAirport hub = AIRPORT_DB.get(hubCode);
            if (hub != null && !hub.getCode().equals(dest.getCode())) {
                flights.add(new PlannerFlight("FL-" + hub.getCode(), hub, dest, now.plusMinutes(10), now.plusHours(10), 5000));
            }
        }
        return flights;
    }

    private void animarRutaCompleta(Order dbOrder, List<PlannerFlight> ruta) throws InterruptedException {
        dbOrder.setStatus("IN_TRANSIT");
        orderRepository.save(dbOrder);
        messagingTemplate.convertAndSend("/topic/orders", "UPDATE");

        for (PlannerFlight vuelo : ruta) {
            System.out.println("✈️ Animando tramo: " + vuelo.getOrigin().getCode() + " -> " + vuelo.getDestination().getCode());

            // 🔥 TRUCO DE TIEMPO: Duración visual 15 segundos
            long duracionVisual = 15; 
            LocalDateTime salida = LocalDateTime.now(); // AHORA MISMO
            LocalDateTime llegada = salida.plusSeconds(duracionVisual);

            Map<String, Object> msg = new HashMap<>();
            msg.put("tipo", "TAKEOFF");
            msg.put("pedidoId", dbOrder.getId());
            
            Map<String, Object> datos = new HashMap<>();
            datos.put("id", vuelo.getCode());
            datos.put("origen", mappearAeropuerto(vuelo.getOrigin()));
            datos.put("destino", mappearAeropuerto(vuelo.getDestination()));
            datos.put("salidaProgramadaISO", salida.toString());
            datos.put("llegadaProgramadaISO", llegada.toString());
            
            msg.put("datos", datos);
            messagingTemplate.convertAndSend("/topic/flights", msg);

            Thread.sleep(duracionVisual * 1000); // Esperar a que llegue visualmente

            Map<String, Object> fin = new HashMap<>();
            fin.put("tipo", "LANDING");
            fin.put("vueloId", vuelo.getCode());
            messagingTemplate.convertAndSend("/topic/flights", fin);
            
            Thread.sleep(1000); // Pequeña pausa entre escalas
        }

        dbOrder.setStatus("COMPLETED");
        orderRepository.save(dbOrder);
        messagingTemplate.convertAndSend("/topic/orders", "UPDATE");
    }

    private Map<String, Object> mappearAeropuerto(PlannerAirport ap) {
        return Map.of("codigo", ap.getCode(), "latitude", ap.getLatitude(), "longitude", ap.getLongitude());
    }
}