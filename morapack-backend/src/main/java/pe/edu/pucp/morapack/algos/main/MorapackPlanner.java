package pe.edu.pucp.morapack.algos.main;

import pe.edu.pucp.morapack.algos.algorithm.IOptimizer;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlanner;
import pe.edu.pucp.morapack.model.*;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.data.DataLoader;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MorapackPlanner {
    private static final String DEFAULT_AIRPORTS_FILE = "data/airports.txt";
    private static final String DEFAULT_FLIGHTS_FILE = "data/flights.csv";
    private static final String DEFAULT_ORDERS_FILE = "data/pedidos.csv";

    public static void main(String[] args) {
        String airportsFile = args.length > 0 ? args[0] : DEFAULT_AIRPORTS_FILE;
        String flightsFile = args.length > 1 ? args[1] : DEFAULT_FLIGHTS_FILE;
        String ordersFile = args.length > 2 ? args[2] : DEFAULT_ORDERS_FILE;

        System.out.println("--- Iniciando Prueba del Planificador MoraPack ---");
        System.out.println("Usando archivos:");
        System.out.println("- Aeropuertos: " + airportsFile);
        System.out.println("- Vuelos: " + flightsFile);
        System.out.println("- Órdenes: " + ordersFile);

        try {
            // Load airports
            List<Airport> airports = DataLoader.loadAirports(airportsFile);
            System.out.println("Loaded " + airports.size() + " airports");

            // Create map for quick airport lookup
            Map<String, Airport> airportMap = airports.stream()
                .collect(Collectors.toMap(Airport::getCode, a -> a));

            // Load flights
            List<Flight> availableFlights = DataLoader.loadFlights(flightsFile, airportMap);
            System.out.println("Loaded " + availableFlights.size() + " flights");

            // Create or load orders
            List<Order> pendingOrders = DataLoader.loadOrders(ordersFile, airportMap);
            System.out.println("Loaded " + pendingOrders.size() + " orders");
            
            System.out.println("\n[INITIAL INPUT DATA]");
            pendingOrders.forEach(p -> 
                System.out.println("  - Order #" + p.getId() + ": " + p.getTotalQuantity() + 
                                 " products to " + p.getDestination().getCity() + 
                                 " (Deadline: " + p.getMaxDeliveryHours() + "h)")
            );

            System.out.println("\n--- Partitioning Orders into Shipments ---");
            List<Shipment> shipmentsToSchedule = partitionOrdersIntoShipments(pendingOrders, availableFlights);
            
            System.out.println("Total of Orders partitioned into " + shipmentsToSchedule.size() + 
                             " shipments to schedule:");
            shipmentsToSchedule.forEach(s -> 
                System.out.println("  - Shipment ID: " + s.getId() + " (from Order #" + 
                                 s.getParentOrder().getId() + "), Products: " + 
                                 s.getQuantity())
            );

            // Create planner and execute
            IOptimizer planner = new TabuSearchPlanner();
            Solution solution = planner.optimize(pendingOrders, availableFlights, airports);

            // Print results
            System.out.println("\n[PLANNING RESULT]");
            printSolution(solution);

        } catch (IOException e) {
            System.err.println("Error loading data files: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        System.out.println("\n--- Test Finished ---");
    }



    private static List<Shipment> partitionOrdersIntoShipments(List<Order> orders, List<Flight> availableFlights) {
        List<Shipment> shipments = new ArrayList<>();
        int shipmentIdCounter = 100;

        for (Order o : orders) {
            int remainingQuantity = o.getTotalQuantity();

            // Strategy: Try to fill direct flights first
            List<Flight> directFlights = availableFlights.stream()
                .filter(f -> f.getOrigin().equals(o.getOrigin()) && f.getDestination().equals(o.getDestination()))
                .sorted(Comparator.comparingInt(Flight::getCapacity).reversed())
                .collect(Collectors.toList());

            for (Flight direct : directFlights) {
                if (remainingQuantity > 0) {
                    int quantityToShip = Math.min(remainingQuantity, direct.getCapacity());
                    shipments.add(new Shipment(shipmentIdCounter++, o, quantityToShip, o.getOrigin(), o.getDestination()));
                    remainingQuantity -= quantityToShip;
                }
            }

            while (remainingQuantity > 0) {
                int referenceCapacity = 200; // Reference capacity for multi-stop routes
                int quantityInThisShipment = Math.min(remainingQuantity, referenceCapacity);
                shipments.add(new Shipment(shipmentIdCounter++, o, quantityInThisShipment, o.getOrigin(), o.getDestination()));
                remainingQuantity -= quantityInThisShipment;
            }
        }
        return shipments;
    }

    private static void printSolution(Solution solution) {
        if (solution != null && !solution.getRouteMap().isEmpty()) {
            System.out.println("Solution found!");
            solution.getRouteMap().forEach((shipment, route) -> {
                System.out.println("\n  > For Shipment #" + shipment.getId() + 
                                 " (from Order #" + shipment.getParentOrder().getId() + 
                                 " with " + shipment.getQuantity() + " products)");
                if (route.getSegments().isEmpty()) {
                    System.out.println("    Route: Could not assign a route!");
                } else {
                    String routeStr = route.getSegments().stream()
                        .map(s -> s.getFlight().getCode() + " (" + 
                                s.getFlight().getOrigin().getName() + " -> " + 
                                s.getFlight().getDestination().getName() + ")")
                        .collect(Collectors.joining(" | "));
                    System.out.println("    Route: " + routeStr);
                    System.out.println("    Estimated arrival: " + route.getFinalArrivalTime());
                }
            });
        } else {
            System.out.println("Could not find a solution.");
        }
    }
}