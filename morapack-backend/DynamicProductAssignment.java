// ENFOQUE MEJORADO: Asignación dinámica de productos

public class DynamicProductAssignment {
    
    public Solution assignProductsToFlights(List<Order> orders, List<Flight> flights) {
        Solution solution = new Solution();
        
        // 1. Crear pool de productos pendientes por orden
        Map<Order, Integer> pendingProducts = new HashMap<>();
        for (Order order : orders) {
            pendingProducts.put(order, order.getTotalQuantity());
        }
        
        // 2. Ordenar órdenes por prioridad (más antiguas primero)
        List<Order> prioritizedOrders = orders.stream()
            .sorted(Comparator.comparing(Order::getOrderTime))
            .collect(Collectors.toList());
            
        // 3. Para cada orden con productos pendientes
        for (Order order : prioritizedOrders) {
            while (pendingProducts.get(order) > 0) {
                
                // 4. Encontrar el MEJOR vuelo disponible para esta orden
                Flight bestFlight = findBestAvailableFlightForOrder(flights, order, solution);
                
                if (bestFlight == null) {
                    break; // No hay más vuelos disponibles para esta orden
                }
                
                // 5. Calcular cuántos productos asignar dinámicamente
                int availableCapacity = bestFlight.getCapacity() - getCurrentLoad(bestFlight, solution);
                int productsToAssign = Math.min(
                    pendingProducts.get(order), 
                    availableCapacity
                );
                
                // 6. Crear shipment SOLO si vale la pena (min 30% utilización)
                if (productsToAssign >= bestFlight.getCapacity() * 0.3) {
                    Shipment newShipment = new Shipment(order, productsToAssign);
                    
                    // Buscar ruta existente o crear nueva
                    PlannerRoute route = findOrCreateRoute(solution, bestFlight, order);
                    route.addShipment(newShipment);
                    
                    // Actualizar productos pendientes
                    pendingProducts.put(order, pendingProducts.get(order) - productsToAssign);
                }
            }
        }
        
        return solution;
    }
    
    private Flight findBestAvailableFlightForOrder(List<Flight> flights, Order order, Solution currentSolution) {
        return flights.stream()
            .filter(f -> isFlightCompatible(f, order))
            .filter(f -> hasAvailableCapacity(f, currentSolution))
            .max(Comparator.comparing(f -> calculateFlightScore(f, order, currentSolution)))
            .orElse(null);
    }
    
    private double calculateFlightScore(Flight flight, Order order, Solution solution) {
        // Factores: utilización esperada, tiempo al deadline, costo, etc.
        double utilizationScore = calculateUtilizationScore(flight, solution);
        double timeScore = calculateTimeScore(flight, order);
        double costScore = calculateCostScore(flight, order);
        
        return 0.4 * utilizationScore + 0.3 * timeScore + 0.3 * costScore;
    }
}