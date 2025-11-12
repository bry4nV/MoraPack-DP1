package pe.edu.pucp.morapack.algos.algorithm.tabu;

import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import pe.edu.pucp.morapack.algos.algorithm.IOptimizer;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;
import pe.edu.pucp.morapack.algos.utils.RouteOption;
import pe.edu.pucp.morapack.algos.utils.AirportStorageManager;
import pe.edu.pucp.morapack.algos.algorithm.tabu.moves.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del algoritmo Tabu Search para planificación de rutas con PlannerShipments.
 * 
 * FLUJO:
 * 1. Greedy dinámico: Asigna productos a rutas de manera eficiente
 * 2. Tabu Search: Mejora la solución mediante movimientos (Split, Merge, Transfer, Reroute)
 * 3. Validación: Verifica restricciones y calcula métricas finales
 */
@Service
public class TabuSearchPlanner implements IOptimizer {
    // Configuración del algoritmo
    private TabuSearchConfig config;
    
    // Main hubs para conexiones
    private static final String LIMA_CODE = "SPIM";
    private static final String BRUSSELS_CODE = "EBCI";
    private static final String BAKU_CODE = "UBBB";
    
    // Métricas del algoritmo
    private double averageDeliveryTimeMinutes = 0.0;
    private int totalIterations = 0;
    private int improvementIterations = 0;
    private int nextShipmentId = 1;
    
    // Estadísticas de movimientos
    private int splitMovesApplied = 0;
    private int mergeMovesApplied = 0;
    private int transferMovesApplied = 0;
    private int rerouteMovesApplied = 0;
    
    // Generador de números aleatorios
    private Random random;
    private long randomSeed;
    
    // Optional external listener for snapshots / stop requests
    private TabuSearchListener listener = null;
    private long snapshotMs = 1000; // heartbeat default
    private long lastSnapshotTime = 0;
    private long snapshotCounter = 0;

    /**
     * Constructor por defecto: Usa timestamp para VARIABILIDAD en cada ejecución
     */
    public TabuSearchPlanner() {
        this(System.currentTimeMillis());
    }
    
    /**
     * Constructor con semilla: Para REPRODUCIBILIDAD cuando se necesite
     * @param seed Semilla para el generador aleatorio
     */
    public TabuSearchPlanner(long seed) {
        this.randomSeed = seed;
        this.random = new Random(seed);
        initializeTabuSearchComponents();
        System.out.println("[RANDOM] Tabu Search initialized with seed: " + seed);
    }

    /**
     * Sanitize airport coordinates to avoid malformed values from CSVs (e.g. DMS entered as integer)
     * This will attempt a best-effort DMS -> decimal conversion when values are obviously out of range
     * and will clamp to valid ranges otherwise. Because PlannerAirport doesn't expose setters for
     * latitude/longitude we apply changes by reflection and log any corrections.
     */
    private void sanitizeAirports(List<PlannerAirport> airports) {
        if (airports == null) return;
        for (PlannerAirport a : airports) {
            double lat = a.getLatitude();
            double lon = a.getLongitude();
            double origLat = lat, origLon = lon;
            boolean changed = false;

            if (!Double.isFinite(lat) || Math.abs(lat) > 90) {
                // Try DMS-like conversion (e.g. 245400 -> 24°54'00" => 24.9)
                double conv = tryConvertDmsLike(lat);
                if (Double.isFinite(conv) && Math.abs(conv) <= 90) {
                    lat = conv;
                    changed = true;
                } else {
                    // Clamp to valid range
                    lat = Math.max(-90.0, Math.min(90.0, lat));
                    changed = true;
                }
            }

            if (!Double.isFinite(lon) || Math.abs(lon) > 180) {
                double conv = tryConvertDmsLike(lon);
                if (Double.isFinite(conv) && Math.abs(conv) <= 180) {
                    lon = conv;
                    changed = true;
                } else {
                    lon = Math.max(-180.0, Math.min(180.0, lon));
                    changed = true;
                }
            }

            if (changed) {
                try {
                    java.lang.reflect.Field latField = PlannerAirport.class.getDeclaredField("latitude");
                    java.lang.reflect.Field lonField = PlannerAirport.class.getDeclaredField("longitude");
                    latField.setAccessible(true);
                    lonField.setAccessible(true);
                    latField.setDouble(a, lat);
                    lonField.setDouble(a, lon);
                    System.out.println(String.format("[DATA] Sanitized airport %s: lat %.6f -> %.6f, lon %.6f -> %.6f",
                        a.getCode(), origLat, lat, origLon, lon));
                } catch (Exception ex) {
                    System.out.println(String.format("[DATA] Failed to sanitize airport %s: %s", a.getCode(), ex.getMessage()));
                }
            }
        }
    }

    /**
     * Try to convert DMS-like numeric formats to decimal degrees.
     * Heuristic: a value like 245400 -> 24°54'00" -> 24.9
     */
    private double tryConvertDmsLike(double v) {
        if (!Double.isFinite(v)) return Double.NaN;
        double sign = v < 0 ? -1.0 : 1.0;
        double abs = Math.abs(v);
        // Only attempt if magnitude suggests DMS without decimals (>= 10000, e.g. DDMMSS)
        if (abs >= 10000 && abs < 10000000) {
            try {
                long iv = (long) Math.round(abs);
                int deg = (int) (iv / 10000);
                int min = (int) ((iv - deg * 10000) / 100);
                int sec = (int) (iv - deg * 10000 - min * 100);
                double dec = deg + min / 60.0 + sec / 3600.0;
                return sign * dec;
            } catch (Exception ignored) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    /**
     * Register a listener to receive snapshots and allow stop requests.
     */
    public void setListener(TabuSearchListener listener, long snapshotMs) {
        this.listener = listener;
        if (snapshotMs > 0) this.snapshotMs = snapshotMs;
    }
    
    private void initializeTabuSearchComponents() {
        this.config = new TabuSearchConfig(
            20,     // tabuListSize inicial (se adapta dinámicamente)
            250,    // maxIterations (reducido porque ahora hay 168 iteraciones vs 84)
            42,     // maxIterationsWithoutImprovement (ajustado proporcionalmente)
            70,     // directRouteProbability
            25,     // oneStopRouteProbability
            1000,   // bottleneckCapacity
            25000,  // capacityViolationPenalty
            35000,  // emptyRoutePenalty
            10000,  // delayBasePenalty
            300,    // delayHourPenalty
            600,    // stopoverPenalty
            22000,  // invalidStopoverTimePenalty
            50000,  // cancellationPenalty
            15000   // replanificationPenalty
        );
    }

    @Override  
    public Solution optimize(List<PlannerOrder> orders, List<PlannerFlight> flights, List<PlannerAirport> airports) {
        if (orders == null || orders.isEmpty()) return new TabuSolution();

        // Sanitize input data (fix malformed coordinates that may come from CSVs)
        sanitizeAirports(airports);
        
        long startTime = System.currentTimeMillis();
        System.out.println("\n" + "=".repeat(80));
        System.out.println("=== TABU SEARCH PLANNER - DYNAMIC SHIPMENT ALLOCATION ===");
        System.out.println("=".repeat(80));
        System.out.println("[ORDERS] To process: " + orders.size());
        System.out.println("[FLIGHTS] Available: " + flights.size());
        System.out.println("[AIRPORTS] Total: " + airports.size());
        System.out.println();
        
        // FASE 1: Generar solución inicial con greedy dinámico
        TabuSolution currentSolution = generateInitialSolutionDynamic(orders, flights, airports);
        TabuSolution bestSolution = new TabuSolution(currentSolution);

        // Demo fallback: if greedy assigned nothing, inject a tiny synthetic shipment so
        // the Tabu phase and the frontend have at least one itinerario to display.
        if ((currentSolution.getPlannerShipments() == null || currentSolution.getPlannerShipments().isEmpty())
                && orders != null && !orders.isEmpty() && flights != null && !flights.isEmpty()) {
            try {
                System.out.println("[TABU][DEMO-FALLBACK] No shipments produced by greedy. Injecting fallback shipment.");
                PlannerOrder firstOrder = orders.get(0);
                // Prefer a direct flight matching origin->destination and time window
                PlannerFlight chosen = null;
                for (PlannerFlight f : flights) {
                    if (f.getOrigin().equals(firstOrder.getOrigin()) && f.getDestination().equals(firstOrder.getDestination())
                            && isValidDepartureTime(firstOrder, f)) {
                        chosen = f;
                        break;
                    }
                }
                // Otherwise pick a flight from the same origin that goes to the correct destination
                if (chosen == null) {
                    for (PlannerFlight f : flights) {
                        // Must match both origin AND destination
                        if (f.getOrigin().equals(firstOrder.getOrigin()) && 
                            f.getDestination().equals(firstOrder.getDestination())) { 
                            chosen = f; 
                            break; 
                        }
                    }
                }
                // DISABLED: Don't send to wrong destinations
                // Last resort: any flight from same origin (even if destination doesn't match - better than nothing)
                // if (chosen == null) {
                //     for (PlannerFlight f : flights) {
                //         if (f.getOrigin().equals(firstOrder.getOrigin())) { 
                //             System.out.println("[TABU][DEMO-FALLBACK][WARNING] No direct flight found, using indirect: " + 
                //                 f.getOrigin().getCode() + "->" + f.getDestination().getCode() + 
                //                 " (order wants: " + firstOrder.getDestination().getCode() + ")");
                //             chosen = f; 
                //             break; 
                //         }
                //     }
                // }
                // if (chosen == null) chosen = flights.get(0);
                
                // If no valid flight found, don't force wrong destination
                if (chosen == null) {
                    System.out.println("[TABU][DEMO-FALLBACK] ❌ No valid flight found for order #" + firstOrder.getId() + 
                        " (" + firstOrder.getOrigin().getCode() + "→" + firstOrder.getDestination().getCode() + ")");
                    System.out.println("[TABU][DEMO-FALLBACK] Order will remain PENDING until correct flight is available.");
                    // Don't create invalid shipment
                    return currentSolution;
                }

                int demoQty = Math.max(1, Math.min(firstOrder.getTotalQuantity(), 10));
                PlannerShipment demoShipment = new PlannerShipment(nextShipmentId++, firstOrder, List.of(chosen), demoQty);
                currentSolution.addPlannerShipment(demoShipment);
                // Refresh bestSolution copy to include the injected shipment
                bestSolution = new TabuSolution(currentSolution);
                System.out.println(String.format("[TABU][DEMO-FALLBACK] Injected shipment id=%d order=%d qty=%d route=%s->%s",
                        demoShipment.getId(), firstOrder.getId(), demoQty, chosen.getOrigin().getCode(), chosen.getDestination().getCode()));
            } catch (Exception ex) {
                System.out.println("[TABU][DEMO-FALLBACK] Failed to inject fallback shipment: " + ex.getMessage());
            }
        }
        
        double initialCost = TabuSearchPlannerCostFunction.calculateCost(
            currentSolution, flights, airports, 0, config.getMaxIterations());
        System.out.println("\n[OK] Initial solution generated:");
        System.out.println("   Cost: " + String.format("%.2f", initialCost));
        printSolutionSummary(currentSolution);

        // Emit an immediate snapshot of the initial solution so listeners (e.g. the STOMP bridge)
        // receive at least one payload even if the Tabu loop finds no candidate moves.
        if (listener != null) {
            try {
                long now = System.currentTimeMillis();
                snapshotCounter++;
                lastSnapshotTime = now;
                listener.onSnapshot(new TabuSolution(currentSolution), 0, initialCost, snapshotCounter, java.time.Instant.ofEpochMilli(now));
            } catch (Exception ex) {
                System.out.println("[TABU] Warning: failed to emit initial snapshot: " + ex.getMessage());
            }
        }
        
        // FASE 2: Optimización con Tabu Search
        System.out.println("\n" + "=".repeat(80));
        System.out.println("=== STARTING TABU SEARCH OPTIMIZATION ===");
        System.out.println("=".repeat(80));
        System.out.println("[CONFIG] Max iterations: " + config.getMaxIterations());
        System.out.println("[CONFIG] Max iterations without improvement: " + config.getMaxIterationsWithoutImprovement());
        System.out.println("[CONFIG] Tabu list size: " + config.getTabuListSize());
        System.out.println("\n[LEGEND]");
        System.out.println("   Status: [OK]=Improving [>>]=Searching [...]=Waiting [!!]=Stale [XX]=Critical");
        System.out.println("   Trend:  [vv]=Decreasing [v]=Slight decrease [==]=Stable [^]=Increasing");
        System.out.println();
        
        Set<String> tabuSet = new HashSet<>();
        int tabuSetMaxSize = config.getTabuListSize();
        int iterationsWithoutImprovement = 0;
        totalIterations = 0;
        
        // Resetear estadísticas de movimientos
        splitMovesApplied = 0;
        mergeMovesApplied = 0;
        transferMovesApplied = 0;
        rerouteMovesApplied = 0;
        
        double bestCostEver = initialCost;
        List<Double> costHistory = new ArrayList<>();
        costHistory.add(initialCost);
        
     while (totalIterations < config.getMaxIterations() && 
         iterationsWithoutImprovement < config.getMaxIterationsWithoutImprovement()) {
            boolean improvedThisIteration = false;
            
            // Generar movimientos candidatos
            List<TabuMoveBase> candidateMoves = generateCandidateMoves(currentSolution, flights, airports);
            
            if (candidateMoves.isEmpty()) {
                System.out.println("[WARNING] No candidate moves available. Stopping.");
                break;
            }
            
            // SHUFFLE para variabilidad en cada ejecución
            Collections.shuffle(candidateMoves, random);
            
            // Encontrar mejor movimiento no-tabú
            TabuMoveBase bestMove = null;
            double bestMoveCost = Double.MAX_VALUE;
            
            for (TabuMoveBase move : candidateMoves) {
                String moveKey = move.getMoveKey();
                
                // Skip si está en lista tabú
                if (tabuSet.contains(moveKey)) {
                    continue;
                }
                
                // Simular movimiento
                TabuSolution testSolution = new TabuSolution(currentSolution);
                move.apply(testSolution);
                
                // VALIDAR: Verificar que la solución respeta capacidades de aeropuertos
                if (!isValidSolution(testSolution, airports)) {
                    continue;  // Skip este movimiento, viola capacidades
                }
                
                // Calcular costo
                double moveCost = TabuSearchPlannerCostFunction.calculateCost(
                    testSolution, flights, airports, totalIterations, config.getMaxIterations());
                
                if (moveCost < bestMoveCost) {
                    bestMoveCost = moveCost;
                    bestMove = move;
                }
            }
            
            // Si encontramos un movimiento válido, aplicarlo
            if (bestMove != null) {
                // Contar tipo de movimiento
                String moveType = bestMove.getMoveType();
                switch (moveType) {
                    case "SPLIT": splitMovesApplied++; break;
                    case "MERGE": mergeMovesApplied++; break;
                    case "TRANSFER": transferMovesApplied++; break;
                    case "REROUTE": rerouteMovesApplied++; break;
                }
                
                bestMove.apply(currentSolution);
                
                // Agregar a lista tabú
                tabuSet.add(bestMove.getMoveKey());
                if (tabuSet.size() > tabuSetMaxSize) {
                    // Eliminar el más antiguo (simplificado - en producción usar cola)
                    Iterator<String> it = tabuSet.iterator();
                    if (it.hasNext()) {
                        it.next();
                        it.remove();
                    }
                }
                
                // Evaluar si mejora la mejor solución
                double currentCost = TabuSearchPlannerCostFunction.calculateCost(
                    currentSolution, flights, airports, totalIterations, config.getMaxIterations());
                double bestCost = TabuSearchPlannerCostFunction.calculateCost(
                    bestSolution, flights, airports, totalIterations, config.getMaxIterations());
                
                costHistory.add(currentCost);
                
                if (currentCost < bestCost) {
                    bestSolution = new TabuSolution(currentSolution);
                    iterationsWithoutImprovement = 0;
                    improvementIterations++;
                    improvedThisIteration = true;
                    
                    double stepImprovement = ((bestCost - currentCost) / bestCost) * 100;
                    double totalImprovement = ((initialCost - currentCost) / initialCost) * 100;
                    bestCostEver = currentCost;
                    
                    // ADAPTATIVO: Reducir tamaño de lista tabú al encontrar mejora (intensificación)
                    if (tabuSetMaxSize > 20) {
                        tabuSetMaxSize = 20;
                        System.out.println("   [DOWN] Tabu list reduced to " + tabuSetMaxSize + " (intensification)");
                    }
                    
                    // Mensaje de mejora con detalles
                    String improvementIcon = stepImprovement > 5 ? "[***]" : (stepImprovement > 1 ? "[**]" : "[*]");
                    System.out.println(String.format("%s Iter %4d: NEW BEST! %.2f -> %.2f (Step: -%.2f%%, Total: %.2f%%) | Move: %s", 
                        improvementIcon, totalIterations, bestCost, currentCost, stepImprovement, totalImprovement, moveType));
                } else {
                    iterationsWithoutImprovement++;
                    
                    // ADAPTATIVO: Aumentar tamaño de lista tabú si hay estancamiento (diversificación)
                    if (iterationsWithoutImprovement == 40 && tabuSetMaxSize < 30) {
                        tabuSetMaxSize = 30;
                        System.out.println("   [UP] Tabu list increased to " + tabuSetMaxSize + " (diversification - stagnation detected)");
                    }
                }
            }
            
            totalIterations++;

            // Check stop request from listener
            if (listener != null && listener.isStopRequested()) {
                System.out.println("[TABU] Stop requested by listener. Exiting optimization loop.");
                break;
            }

            // Emit snapshot on improvement or heartbeat
            long now = System.currentTimeMillis();
            boolean shouldSnapshot = false;
            if (listener != null) {
                // Ensure we have a current cost to compare (recompute if necessary)
                double currentCost = TabuSearchPlannerCostFunction.calculateCost(
                    currentSolution, flights, airports, totalIterations, config.getMaxIterations());

                // If we detected an improvement during this iteration, force a snapshot
                if (improvedThisIteration) {
                    shouldSnapshot = true; // immediate improvement snapshot
                } else if (currentCost < bestCostEver) {
                    // Fallback: if for whatever reason bestCostEver lagged, treat as improvement
                    shouldSnapshot = true;
                }
                if (!shouldSnapshot && now - lastSnapshotTime >= snapshotMs) shouldSnapshot = true;
                if (shouldSnapshot) {
                    lastSnapshotTime = now;
                    snapshotCounter++;
                    try {
                        listener.onSnapshot(new TabuSolution(currentSolution), totalIterations, bestCostEver, snapshotCounter, java.time.Instant.ofEpochMilli(now));
                    } catch (Exception ex) {
                        System.out.println("[TABU] Warning: listener threw exception: " + ex.getMessage());
                    }
                }
            }
            
            // Log periódico mostrando ESTADO DE MEJORA
            if (totalIterations % 20 == 0) {
                double currentCost = TabuSearchPlannerCostFunction.calculateCost(
                    currentSolution, flights, airports, totalIterations, config.getMaxIterations());
                
                // Calcular tendencia
                String trendIcon = getTrendIcon(costHistory, currentCost);
                String statusIcon = getStatusIcon(iterationsWithoutImprovement);
                
                // Calcular % de mejora total
                double totalImprovement = ((initialCost - bestCostEver) / initialCost) * 100;
                
                // Racha de mejoras
                
                System.out.println(String.format("%s %s Iter %4d/%d | Current: %.2f | Best: %.2f %s | Improved: %.1f%% | Stale: %d/%d | Tabu: %d",
                    statusIcon, trendIcon, 
                    totalIterations, config.getMaxIterations(),
                    currentCost, bestCostEver,
                    getImprovementBadge(totalImprovement),
                    totalImprovement,
                    iterationsWithoutImprovement, config.getMaxIterationsWithoutImprovement(),
                    tabuSetMaxSize));
            }
            
            // Advertencia si no hay mejora por mucho tiempo
            if (iterationsWithoutImprovement > 0 && iterationsWithoutImprovement % 30 == 0) {
                double staleness = (double) iterationsWithoutImprovement / config.getMaxIterationsWithoutImprovement() * 100;
                System.out.println(String.format("[WARNING] STAGNATION: %d iterations without improvement (%.0f%% to stop limit)",
                    iterationsWithoutImprovement, staleness));
            }
            
            // Celebrar hitos de mejora
            if (improvementIterations > 0 && improvementIterations % 10 == 0 && iterationsWithoutImprovement == 0) {
                System.out.println(String.format("[HOT] STREAK: %d improvements found! Keep going!", improvementIterations));
            }
        }
        
        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;
        
        // Calcular métricas finales
        calculateFinalMetrics(bestSolution);
        
        // Imprimir resultados finales
        printFinalResults(bestSolution, flights, airports, executionTime, initialCost, bestCostEver, costHistory);

        return bestSolution;
    }
    
    /**
     * Obtener tendencia del costo
     */
    private String getTrendIcon(List<Double> costHistory, double currentCost) {
        if (costHistory.size() < 5) return "[STABLE]";
        
        // Comparar con las últimas 5 iteraciones
        double recentAvg = 0;
        int count = 0;
        for (int i = Math.max(0, costHistory.size() - 5); i < costHistory.size(); i++) {
            recentAvg += costHistory.get(i);
            count++;
        }
        recentAvg /= count;
        
        if (currentCost < recentAvg * 0.98) {
            return "[DOWN--]"; // Bajando significativamente
        } else if (currentCost < recentAvg * 0.995) {
            return "[DOWN-]"; // Bajando ligeramente
        } else if (currentCost > recentAvg * 1.005) {
            return "[UP]"; // Subiendo
        } else {
            return "[STABLE]"; // Estable
        }
    }
    
    /**
     * Obtener estado según iteraciones sin mejora
     */
    private String getStatusIcon(int iterationsWithoutImprovement) {
        if (iterationsWithoutImprovement == 0) {
            return "[IMPROVING]"; // Mejorando
        } else if (iterationsWithoutImprovement < 10) {
            return "[SEARCHING]"; // Buscando
        } else if (iterationsWithoutImprovement < 30) {
            return "[WAITING]"; // Esperando
        } else if (iterationsWithoutImprovement < 50) {
            return "[WARNING]"; // Advertencia
        } else {
            return "[CRITICAL]"; // Crítico
        }
    }
    
    /**
     * Obtener clasificación de mejora
     */
    private String getImprovementBadge(double improvementPercentage) {
        if (improvementPercentage > 50) {
            return "[EXCELLENT]"; // Excelente
        } else if (improvementPercentage > 25) {
            return "[VERY-GOOD]"; // Muy bueno
        } else if (improvementPercentage > 10) {
            return "[GOOD]"; // Bueno
        } else if (improvementPercentage > 5) {
            return "[MODERATE]"; // Moderado
        } else if (improvementPercentage > 0) {
            return "[SLIGHT]"; // Leve
        } else {
            return ""; // Sin mejora
        }
    }
    
    // ========== VALIDACIÓN DE CAPACIDADES ==========
    
    /**
     * Verifica si una solución respeta las capacidades de aeropuertos.
     * Calcula las cargas proyectadas en cada aeropuerto y verifica que no excedan capacidades.
     * 
     * @param solution Solución a validar
     * @param airports Lista de aeropuertos
     * @return true si la solución es válida (no excede capacidades)
     */
    private boolean isValidSolution(TabuSolution solution, List<PlannerAirport> airports) {
        // Crear un mapa de aeropuertos por código para acceso rápido
        Map<String, PlannerAirport> airportMap = new HashMap<>();
        for (PlannerAirport airport : airports) {
            airportMap.put(airport.getCode(), airport);
        }
        
        // Calcular carga máxima proyectada en cada aeropuerto
        Map<String, Integer> maxLoad = new HashMap<>();
        
        for (PlannerShipment shipment : solution.getPlannerShipments()) {
            List<PlannerFlight> flights = shipment.getFlights();
            int quantity = shipment.getQuantity();
            
            // Para cada aeropuerto intermedio (no el destino final)
            for (int i = 0; i < flights.size() - 1; i++) {
                PlannerFlight flight = flights.get(i);
                String airportCode = flight.getDestination().getCode();
                
                // Acumular carga en este aeropuerto (incluyendo hubs)
                // NOTA: Hubs tienen producción ilimitada, pero capacidad física limitada
                maxLoad.merge(airportCode, quantity, Integer::sum);
            }
        }
        
        // Verificar que ningún aeropuerto exceda su capacidad
        for (Map.Entry<String, Integer> entry : maxLoad.entrySet()) {
            String code = entry.getKey();
            int load = entry.getValue();
            
            PlannerAirport airport = airportMap.get(code);
            if (airport != null) {
                if (load > airport.getStorageCapacity()) {
                    // Solución inválida: excede capacidad
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // ========== GREEDY DINÁMICO ==========
    
    /**
     * Verifica si una ruta puede acomodar la cantidad de productos
     * considerando las capacidades de los aeropuertos intermedios.
     * 
     * @param route La ruta a verificar
     * @param quantity Cantidad de productos a transportar
     * @param airportManager Gestor de capacidades de aeropuertos
     * @return true si todos los aeropuertos de escala tienen capacidad
     */
    private boolean canRouteAccommodateAirportCapacity(RouteOption route, int quantity, AirportStorageManager airportManager) {
        List<PlannerFlight> flights = route.getFlights();
        
        // Verificar cada aeropuerto intermedio (no el destino final)
        for (int i = 0; i < flights.size() - 1; i++) {
            PlannerFlight flight = flights.get(i);
            PlannerAirport destination = flight.getDestination();
            
            // Verificar si el aeropuerto puede acomodar la cantidad (incluyendo hubs)
            // NOTA: Hubs tienen producción ilimitada, pero capacidad física limitada
            if (!airportManager.hasAvailableCapacity(destination, quantity)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Actualiza las capacidades de los aeropuertos después de asignar productos a una ruta
     * 
     * @param route La ruta por la que se transportan productos
     * @param quantity Cantidad de productos asignados
     * @param airportManager Gestor de capacidades
     */
    private void updateAirportCapacities(RouteOption route, int quantity, AirportStorageManager airportManager) {
        List<PlannerFlight> flights = route.getFlights();
        
        // Reservar capacidad física en cada aeropuerto intermedio
        // NOTA: Main hubs tienen PRODUCCIÓN ilimitada, pero CAPACIDAD FÍSICA limitada
        // Todos los aeropuertos tienen límite de espacio de almacenamiento
        for (int i = 0; i < flights.size() - 1; i++) {
            PlannerFlight flight = flights.get(i);
            PlannerAirport destination = flight.getDestination();
            
            // Validar capacidad física del aeropuerto (incluyendo hubs)
            airportManager.reserveCapacity(destination, quantity);
        }
    }
    
    /**
     * Genera solución inicial distribuyendo productos dinámicamente entre rutas disponibles
     */
    private TabuSolution generateInitialSolutionDynamic(List<PlannerOrder> orders, List<PlannerFlight> flights, List<PlannerAirport> airports) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("FASE 1: GREEDY DYNAMIC ALLOCATION");
        System.out.println("-".repeat(80));

        TabuSolution solution = new TabuSolution();
        Map<PlannerFlight, Integer> flightCapacityRemaining = new HashMap<>();
        
        // NUEVO: Inicializar gestor de capacidades de aeropuertos
        AirportStorageManager airportManager = new AirportStorageManager(airports);

        // Inicializar capacidades disponibles de vuelos
        for (PlannerFlight flight : flights) {
            flightCapacityRemaining.put(flight, flight.getCapacity());
        }
        
        System.out.println(String.format("[CAPACITY] Initialized airport capacity tracking for %d airports", airports.size()));
        
        // Ordenar pedidos por prioridad (urgencia) con algo de aleatoriedad
        List<PlannerOrder> prioritizedOrders = new ArrayList<>(orders);
        prioritizedOrders.sort((a, b) -> {
            int timeCompare = a.getOrderTime().compareTo(b.getOrderTime());
            if (timeCompare != 0) return timeCompare;
            
            // Agregar pequeña aleatoriedad en órdenes con misma urgencia
            if (Math.abs(a.getMaxDeliveryHours() - b.getMaxDeliveryHours()) < 5) {
                return random.nextBoolean() ? -1 : 1;  // Orden aleatorio si son similares
            }
            return Long.compare(a.getMaxDeliveryHours(), b.getMaxDeliveryHours());
        });
        
        int ordersProcessed = 0;
        int totalProductsAssigned = 0;
        
        int debugOrders = 10; // only print verbose debug for first N orders to avoid noisy logs
        int debugCount = 0;
        for (PlannerOrder order : prioritizedOrders) {
            System.out.println(String.format("\nProcessing Order #%d: %d products, %s → %s, deadline: %d hours",
                order.getId(), order.getTotalQuantity(), 
                order.getOrigin().getCode(), order.getDestination().getCode(),
                order.getMaxDeliveryHours()));
            
            int remainingProducts = order.getTotalQuantity();
            List<PlannerShipment> orderShipments = new ArrayList<>();
            
            // 1. Intentar rutas directas PRIMERO
            List<RouteOption> directRoutes = findDirectRoutes(order, flights, flightCapacityRemaining);
            
            // DIVERSIDAD: Mezclar rutas para no siempre elegir las mismas
            if (directRoutes.size() > 1) {
                Collections.shuffle(directRoutes.subList(0, Math.min(5, directRoutes.size())), random);
            }
            
            for (RouteOption route : directRoutes) {
                if (remainingProducts <= 0) break;
                
                int toAssign = Math.min(remainingProducts, route.getMinCapacity());
                if (toAssign > 0) {
                    // ✅ VERIFICAR capacidad de aeropuertos antes de asignar
                    if (!canRouteAccommodateAirportCapacity(route, toAssign, airportManager)) {
                        continue; // Skip esta ruta, no hay capacidad en aeropuertos
                    }
                    
                    PlannerShipment shipment = new PlannerShipment(
                        nextShipmentId++,
                        order,
                        route.getFlights(),
                        toAssign
                    );
                    orderShipments.add(shipment);
                    updateCapacities(route.getFlights(), toAssign, flightCapacityRemaining);
                    
                    // ✅ ACTUALIZAR capacidades de aeropuertos
                    updateAirportCapacities(route, toAssign, airportManager);
                    
                    remainingProducts -= toAssign;
                    
                    System.out.println(String.format("   Assigned %d products to DIRECT route: %s",
                        toAssign, shipment.getRouteDescription()));
                }
            }
            
            // DEBUG: report candidate route counts and capacities for first few orders
            if (debugCount < debugOrders) {
                System.out.println(String.format("   DEBUG: directRoutes=%d", directRoutes.size()));
                // print top 3 direct route capacities
                int idx = 0;
                for (RouteOption r : directRoutes) {
                    if (idx++ >= 3) break;
                    System.out.println(String.format("      direct candidate minCap=%d flights=%s", r.getMinCapacity(), r.getFlights().stream().map(f -> f.getOrigin().getCode() + "->" + f.getDestination().getCode()).toList()));
                }
            }

            // 2. Si quedan productos, intentar rutas con CONEXIONES
            if (remainingProducts > 0) {
                List<RouteOption> connectionRoutes = findConnectionRoutes(order, flights, airports, flightCapacityRemaining);
                
                // ✨ DIVERSIDAD: Mezclar rutas con conexión también
                if (connectionRoutes.size() > 1) {
                    Collections.shuffle(connectionRoutes.subList(0, Math.min(5, connectionRoutes.size())), random);
                }
                
                for (RouteOption route : connectionRoutes) {
                    if (remainingProducts <= 0) break;
                    
                    int toAssign = Math.min(remainingProducts, route.getMinCapacity());
                    if (toAssign > 0) {
                        // ✅ VERIFICAR capacidad de aeropuertos antes de asignar
                        if (!canRouteAccommodateAirportCapacity(route, toAssign, airportManager)) {
                            continue; // Skip esta ruta, no hay capacidad en aeropuertos
                        }
                        
                        PlannerShipment shipment = new PlannerShipment(
                            nextShipmentId++,
                            order,
                            route.getFlights(),
                            toAssign
                        );
                        orderShipments.add(shipment);
                        updateCapacities(route.getFlights(), toAssign, flightCapacityRemaining);
                        
                        // ✅ ACTUALIZAR capacidades de aeropuertos
                        updateAirportCapacities(route, toAssign, airportManager);
                        
                        remainingProducts -= toAssign;
                        
                        System.out.println(String.format("   Assigned %d products to CONNECTION route (%d stops): %s",
                            toAssign, route.getNumberOfStops(), shipment.getRouteDescription()));
                    }
                }

                if (debugCount < debugOrders) {
                    System.out.println(String.format("   DEBUG: connectionRoutes=%d", connectionRoutes.size()));
                    int idx2 = 0;
                    for (RouteOption r : connectionRoutes) {
                        if (idx2++ >= 3) break;
                        System.out.println(String.format("      conn candidate minCap=%d flights=%s", r.getMinCapacity(), r.getFlights().stream().map(f -> f.getOrigin().getCode() + "->" + f.getDestination().getCode()).toList()));
                    }
                }

                debugCount++;
            }
            
            // Guardar shipments del pedido
            solution.addAllPlannerShipments(orderShipments);
            
            if (remainingProducts > 0) {
                System.out.println(String.format("   WARNING: %d products NOT assigned (no capacity/route available)",
                    remainingProducts));
            } else {
                ordersProcessed++;
                totalProductsAssigned += order.getTotalQuantity();
            }
        }
        
        solution.setAllOrders(orders);

        System.out.println("\n" + "-".repeat(80));
        System.out.println("GREEDY ALLOCATION COMPLETED");
        System.out.println(String.format("   Orders fully assigned: %d/%d", ordersProcessed, orders.size()));
        System.out.println(String.format("   Total products assigned: %d", totalProductsAssigned));
        System.out.println("-".repeat(80));

        return solution;
    }

    /**
     * Buscar rutas directas disponibles
     */
    private List<RouteOption> findDirectRoutes(PlannerOrder order, List<PlannerFlight> flights, 
                                                Map<PlannerFlight, Integer> capacityRemaining) {
        List<RouteOption> routes = new ArrayList<>();
        
        // 🔍 DEBUG: Contar vuelos candidatos
        int matchingOriginDest = 0;
        int matchingButWrongTime = 0;
        int matchingButNoCapacity = 0;
        
        for (PlannerFlight flight : flights) {
            boolean originMatch = flight.getOrigin().equals(order.getOrigin());
            boolean destMatch = flight.getDestination().equals(order.getDestination());
            boolean timeValid = isValidDepartureTime(order, flight);
            int capacity = capacityRemaining.getOrDefault(flight, 0);
            
            if (originMatch && destMatch) {
                matchingOriginDest++;
                if (!timeValid) {
                    matchingButWrongTime++;
                } else if (capacity == 0) {
                    matchingButNoCapacity++;
                }
            }
            
            if (originMatch && destMatch && timeValid) {
                RouteOption route = new RouteOption(List.of(flight));
                route.setMinCapacity(capacity);
                
                if (route.getMinCapacity() > 0) {
                    routes.add(route);
                }
            }
        }
        
        // 🔍 DEBUG: Reportar hallazgos Y escribir a archivo de log
        if (matchingOriginDest > 0 && routes.isEmpty()) {
            String debugMsg = String.format("   🔍 DEBUG findDirectRoutes: Found %d flights %s→%s but NONE valid:", 
                matchingOriginDest, order.getOrigin().getCode(), order.getDestination().getCode());
            System.out.println(debugMsg);
            System.out.println(String.format("      - Wrong time: %d flights", matchingButWrongTime));
            System.out.println(String.format("      - No capacity: %d flights", matchingButNoCapacity));
            System.out.println(String.format("      - Order time: %s, deadline: %d hours", 
                order.getOrderTime(), order.getMaxDeliveryHours()));
            
            // Write detailed debug info to file
            try (java.io.FileWriter fw = new java.io.FileWriter("flight_debug.log", true);
                 java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                
                pw.println("=".repeat(80));
                pw.println(String.format("ORDER #%d: %d products, %s → %s", 
                    order.getId(), order.getTotalQuantity(), 
                    order.getOrigin().getCode(), order.getDestination().getCode()));
                pw.println(String.format("Order time: %s", order.getOrderTime()));
                pw.println(String.format("Deadline: %d hours (delivery by: %s)", 
                    order.getMaxDeliveryHours(), 
                    order.getOrderTime().plusHours(order.getMaxDeliveryHours())));
                pw.println(String.format("Found %d matching flights but NONE valid", matchingOriginDest));
                pw.println(String.format("  Wrong time: %d | No capacity: %d", 
                    matchingButWrongTime, matchingButNoCapacity));
                pw.println();
                
                // Show ALL matching flights with detailed analysis
                int flightNum = 0;
                for (PlannerFlight flight : flights) {
                    if (flight.getOrigin().equals(order.getOrigin()) && 
                        flight.getDestination().equals(order.getDestination())) {
                        flightNum++;
                        long hoursUntilDep = java.time.temporal.ChronoUnit.HOURS.between(
                            order.getOrderTime(), flight.getDepartureTime());
                        int capacity = capacityRemaining.getOrDefault(flight, 0);
                        boolean timeValid = isValidDepartureTime(order, flight);
                        
                        pw.println(String.format("  Flight #%d: %s", flightNum, flight.getCode()));
                        pw.println(String.format("    Departs: %s (in %d hours from order)", 
                            flight.getDepartureTime(), hoursUntilDep));
                        pw.println(String.format("    Arrives: %s", flight.getArrivalTime()));
                        pw.println(String.format("    Capacity: %d", capacity));
                        pw.println(String.format("    Time valid: %s (0 <= %d <= %d)?", 
                            timeValid, hoursUntilDep, order.getMaxDeliveryHours()));
                        
                        if (!timeValid) {
                            if (hoursUntilDep < 0) {
                                pw.println("    ❌ REJECTED: Flight departs BEFORE order time");
                            } else {
                                pw.println("    ❌ REJECTED: Flight departs AFTER deadline");
                            }
                        } else if (capacity == 0) {
                            pw.println("    ❌ REJECTED: No capacity remaining");
                        }
                        pw.println();
                    }
                }
                pw.println();
                pw.flush();
                
            } catch (Exception e) {
                System.err.println("   ⚠️  Failed to write debug log: " + e.getMessage());
            }
            
            // Show sample flights in console (limited to 3)
            int sampleCount = 0;
            for (PlannerFlight flight : flights) {
                if (flight.getOrigin().equals(order.getOrigin()) && 
                    flight.getDestination().equals(order.getDestination())) {
                    long hoursUntilDep = java.time.temporal.ChronoUnit.HOURS.between(
                        order.getOrderTime(), flight.getDepartureTime());
                    System.out.println(String.format("      Sample flight: departs %s (in %d hours), arrives %s, capacity=%d",
                        flight.getDepartureTime(), hoursUntilDep, flight.getArrivalTime(),
                        capacityRemaining.getOrDefault(flight, 0)));
                    if (++sampleCount >= 3) break;
                }
            }
        }
        
        // Ordenar por prioridad (tiempo, costo)
        routes.sort(RouteOption::compareTo);
        
        return routes;
    }
    
    /**
     * Buscar rutas con conexiones disponibles
     */
    private List<RouteOption> findConnectionRoutes(PlannerOrder order, List<PlannerFlight> flights, 
                                                    List<PlannerAirport> airports,
                                                    Map<PlannerFlight, Integer> capacityRemaining) {
        List<RouteOption> routes = new ArrayList<>();
        String[] hubCodes = {LIMA_CODE, BRUSSELS_CODE, BAKU_CODE};
        
        for (String hubCode : hubCodes) {
            // Buscar vuelo: origen → hub
            List<PlannerFlight> firstLegs = flights.stream()
                .filter(f -> f.getOrigin().equals(order.getOrigin()) &&
                             f.getDestination().getCode().equals(hubCode) &&
                             isValidDepartureTime(order, f))
                .collect(Collectors.toList());
            
            for (PlannerFlight firstLeg : firstLegs) {
                // Buscar vuelo: hub → destino
                List<PlannerFlight> secondLegs = flights.stream()
                    .filter(f -> f.getOrigin().getCode().equals(hubCode) &&
                                 f.getDestination().equals(order.getDestination()) &&
                                 isValidConnection(firstLeg, f))
                    .collect(Collectors.toList());
                
                for (PlannerFlight secondLeg : secondLegs) {
                    List<PlannerFlight> routeFlights = List.of(firstLeg, secondLeg);
                    RouteOption route = new RouteOption(routeFlights);
                    
                    // Capacidad = mínimo de ambos vuelos (cuello de botella)
                    int minCap = Math.min(
                        capacityRemaining.getOrDefault(firstLeg, 0),
                        capacityRemaining.getOrDefault(secondLeg, 0)
                    );
                    route.setMinCapacity(minCap);
                    
                    if (route.getMinCapacity() > 0) {
                        routes.add(route);
                    }
                }
            }
        }
        
        routes.sort(RouteOption::compareTo);
        
        return routes;
    }
    
    private boolean isValidDepartureTime(PlannerOrder order, PlannerFlight flight) {
        long hoursUntilDeparture = ChronoUnit.HOURS.between(order.getOrderTime(), flight.getDepartureTime());
        return hoursUntilDeparture >= 0 && hoursUntilDeparture <= order.getMaxDeliveryHours();
    }

    private boolean isValidConnection(PlannerFlight first, PlannerFlight second) {
        long connectionHours = ChronoUnit.HOURS.between(first.getArrivalTime(), second.getDepartureTime());
        return connectionHours >= 1;  // ✅ Solo mínimo 1 hora (no hay máximo en el enunciado)
    }
    
    private void updateCapacities(List<PlannerFlight> route, int quantity, Map<PlannerFlight, Integer> remaining) {
        for (PlannerFlight flight : route) {
            int current = remaining.get(flight);
            remaining.put(flight, current - quantity);
        }
    }
    
    // ========== TABU SEARCH - GENERACIÓN DE MOVIMIENTOS ==========
    
    /**
     * Generar movimientos candidatos
     */
    private List<TabuMoveBase> generateCandidateMoves(TabuSolution solution, List<PlannerFlight> flights, List<PlannerAirport> airports) {
        List<TabuMoveBase> moves = new ArrayList<>();
        List<PlannerShipment> shipments = solution.getPlannerShipments();
        
        int movesLimit = 50;
        
        for (PlannerShipment shipment : shipments) {
            if (moves.size() >= movesLimit) break;
            
            // 1. Split: Dividir shipment grande
            if (shipment.getQuantity() > 10) {
                // ✨ Puntos de split con variabilidad
                int[] splitPoints = {
                    shipment.getQuantity() / 2,
                    shipment.getQuantity() / 3,
                    shipment.getQuantity() / 4,
                    // Agregar puntos aleatorios para diversificar
                    (int)(shipment.getQuantity() * (0.3 + random.nextDouble() * 0.4))  // 30%-70%
                };
                
                for (int splitQty : splitPoints) {
                    if (splitQty > 0 && splitQty < shipment.getQuantity()) {
                        moves.add(new SplitShipmentMove(shipment, splitQty, nextShipmentId++));
                        if (moves.size() >= movesLimit) break;
                    }
                }
            }
            
            // 2. Merge: Fusionar con otros shipments del mismo order y ruta
            for (PlannerShipment other : shipments) {
                if (shipment.equals(other)) continue;
                if (!shipment.getOrder().equals(other.getOrder())) continue;
                if (!shipment.getFlights().equals(other.getFlights())) continue;
                
                moves.add(new MergeShipmentsMove(shipment, other));
                if (moves.size() >= movesLimit) break;
            }
            
            // 3. Transfer: Mover productos entre shipments del mismo order
            for (PlannerShipment other : shipments) {
                if (shipment.equals(other)) continue;
                if (!shipment.getOrder().equals(other.getOrder())) continue;
                
                // ✨ Cantidad de transferencia con variabilidad
                int maxTransfer = Math.max(1, shipment.getQuantity() / 3);
                int transferQty = random.nextInt(maxTransfer) + 1;  // 1 a maxTransfer
                
                if (transferQty > 0 && transferQty < shipment.getQuantity()) {
                    moves.add(new TransferQuantityMove(shipment, other, transferQty));
                    if (moves.size() >= movesLimit) break;
                }
            }
            
            // 4. Reroute: Cambiar a ruta alternativa
            List<List<PlannerFlight>> alternativeRoutes = findAlternativeRoutes(shipment, flights, airports);
            for (List<PlannerFlight> newRoute : alternativeRoutes) {
                if (!newRoute.equals(shipment.getFlights())) {
                    moves.add(new RerouteShipmentMove(shipment, newRoute));
                    if (moves.size() >= movesLimit) break;
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Encontrar rutas alternativas para un shipment
     */
    private List<List<PlannerFlight>> findAlternativeRoutes(PlannerShipment shipment, List<PlannerFlight> flights, List<PlannerAirport> airports) {
        List<List<PlannerFlight>> alternatives = new ArrayList<>();
        PlannerOrder order = shipment.getOrder();
        
        // Rutas directas
        for (PlannerFlight flight : flights) {
            if (flight.getOrigin().equals(order.getOrigin()) &&
                flight.getDestination().equals(order.getDestination()) &&
                isValidDepartureTime(order, flight)) {
                alternatives.add(List.of(flight));
            }
        }
        
        // Rutas con conexión (limitar a 2 para eficiencia)
        String[] hubCodes = {LIMA_CODE, BRUSSELS_CODE, BAKU_CODE};
        for (String hubCode : hubCodes) {
            for (PlannerFlight firstLeg : flights) {
                if (!firstLeg.getOrigin().equals(order.getOrigin())) continue;
                if (!firstLeg.getDestination().getCode().equals(hubCode)) continue;
                if (!isValidDepartureTime(order, firstLeg)) continue;
                
                for (PlannerFlight secondLeg : flights) {
                    if (!secondLeg.getOrigin().getCode().equals(hubCode)) continue;
                    if (!secondLeg.getDestination().equals(order.getDestination())) continue;
                    if (!isValidConnection(firstLeg, secondLeg)) continue;
                    
                    alternatives.add(List.of(firstLeg, secondLeg));
                }
            }
        }
        
        return alternatives.stream().limit(5).collect(Collectors.toList());
    }
    
    // ========== MÉTRICAS Y REPORTING ==========
    
    private void calculateFinalMetrics(TabuSolution solution) {
        List<PlannerShipment> shipments = solution.getPlannerShipments();
        if (shipments.isEmpty()) {
            this.averageDeliveryTimeMinutes = 0.0;
            return;
        }
        
        double totalMinutes = 0.0;
        int count = 0;
        
        for (PlannerShipment shipment : shipments) {
            long minutes = ChronoUnit.MINUTES.between(
                shipment.getOrder().getOrderTime(),
                shipment.getFinalArrivalTime()
            );
            totalMinutes += minutes;
            count++;
        }
        
        this.averageDeliveryTimeMinutes = count > 0 ? totalMinutes / count : 0.0;
    }
    
    public double getAverageDeliveryTimeMinutes() {
        return averageDeliveryTimeMinutes;
    }
    
    private void printSolutionSummary(TabuSolution solution) {
        Map<String, Object> stats = solution.getStatistics();
        System.out.println("   Total shipments: " + stats.get("totalShipments"));
        System.out.println("   Direct routes: " + stats.get("directShipments"));
        System.out.println("   Connection routes: " + stats.get("connectionShipments"));
        System.out.println("   Total products: " + stats.get("totalProducts"));
    }
    
    private void printFinalResults(TabuSolution solution, List<PlannerFlight> flights, List<PlannerAirport> airports, 
                                    double executionTime, double initialCost, double finalCost, List<Double> costHistory) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("=== OPTIMIZATION COMPLETED ===");
        System.out.println("=".repeat(80));
        
        // Tiempo y rendimiento
        System.out.println("\n⏱️  PERFORMANCE METRICS:");
        System.out.println("   Execution time: " + String.format("%.2f", executionTime) + " seconds");
        System.out.println("   Iterations per second: " + String.format("%.1f", totalIterations / executionTime));
        System.out.println("   Total iterations: " + totalIterations);
        System.out.println("   Iterations with improvement: " + improvementIterations + " (" + 
            String.format("%.1f%%", (double) improvementIterations / totalIterations * 100) + ")");
        
        // Costos
        System.out.println("\nCOST ANALYSIS:");
        double improvement = ((initialCost - finalCost) / initialCost) * 100;
        System.out.println("   Initial cost: " + String.format("%.2f", initialCost));
        System.out.println("   Final cost: " + String.format("%.2f", finalCost));
        System.out.println("   Improvement: " + String.format("%.2f", initialCost - finalCost) + 
            " (" + String.format("%.2f%%", improvement) + ")");
        
        // Movimientos aplicados
        System.out.println("\nMOVES APPLIED:");
        int totalMoves = splitMovesApplied + mergeMovesApplied + transferMovesApplied + rerouteMovesApplied;
        System.out.println("   Split moves: " + splitMovesApplied + " (" + 
            String.format("%.1f%%", totalMoves > 0 ? (double) splitMovesApplied / totalMoves * 100 : 0) + ")");
        System.out.println("   Merge moves: " + mergeMovesApplied + " (" + 
            String.format("%.1f%%", totalMoves > 0 ? (double) mergeMovesApplied / totalMoves * 100 : 0) + ")");
        System.out.println("   Transfer moves: " + transferMovesApplied + " (" + 
            String.format("%.1f%%", totalMoves > 0 ? (double) transferMovesApplied / totalMoves * 100 : 0) + ")");
        System.out.println("   Reroute moves: " + rerouteMovesApplied + " (" + 
            String.format("%.1f%%", totalMoves > 0 ? (double) rerouteMovesApplied / totalMoves * 100 : 0) + ")");
        System.out.println("   TOTAL: " + totalMoves);
        
        // Entrega
        System.out.println("\nDELIVERY METRICS:");
        System.out.println("   Average delivery time: " + String.format("%.2f", averageDeliveryTimeMinutes) + " minutes");
        System.out.println("   Average delivery time: " + String.format("%.2f", averageDeliveryTimeMinutes / 60.0) + " hours");
        
        // Solución
        System.out.println("\nSOLUTION SUMMARY:");
        printSolutionSummary(solution);
        
        // Análisis de completitud de órdenes
        printOrderCompletionAnalysis(solution);
        
        // Gráfico de convergencia (ASCII art simple)
        printCostConvergenceGraph(costHistory, initialCost);
        
        // Detailed report per order
        printDetailedOrderReport(solution);
    }
    
    /**
     * Análisis de completitud de órdenes
     */
    private void printOrderCompletionAnalysis(TabuSolution solution) {
        System.out.println("\nORDER COMPLETION ANALYSIS:");
        System.out.println("   " + "-".repeat(60));
        
        List<PlannerOrder> allOrders = solution.getAllOrders();
        int fullyCompleted = 0;
        int partiallyCompleted = 0;
        int notCompleted = 0;
        int onTime = 0;
        int late = 0;
        
        for (PlannerOrder order : allOrders) {
            List<PlannerShipment> orderShipments = solution.getShipmentsForOrder(order);
            int assignedQty = orderShipments.stream().mapToInt(PlannerShipment::getQuantity).sum();
            int requiredQty = order.getTotalQuantity();
            
            if (assignedQty == 0) {
                notCompleted++;
            } else if (assignedQty < requiredQty) {
                partiallyCompleted++;
            } else {
                fullyCompleted++;
                
                // Verificar si está a tiempo
                boolean allOnTime = orderShipments.stream().allMatch(PlannerShipment::meetsDeadline);
                if (allOnTime) {
                    onTime++;
                } else {
                    late++;
                }
            }
        }
        
        int totalOrders = allOrders.size();
        double completionRate = totalOrders > 0 ? (double) fullyCompleted / totalOrders * 100 : 0;
        double onTimeRate = fullyCompleted > 0 ? (double) onTime / fullyCompleted * 100 : 0;
        
        System.out.println(String.format("   Total orders: %d", totalOrders));
        System.out.println(String.format("   Fully completed: %d (%.1f%%)", fullyCompleted, completionRate));
        System.out.println(String.format("      └─ On time: %d (%.1f%% of completed)", onTime, onTimeRate));
        System.out.println(String.format("      └─ Late: %d (%.1f%% of completed)", late, 
            fullyCompleted > 0 ? (double) late / fullyCompleted * 100 : 0));
        System.out.println(String.format("   Partially completed: %d (%.1f%%)", partiallyCompleted,
            totalOrders > 0 ? (double) partiallyCompleted / totalOrders * 100 : 0));
        System.out.println(String.format("   Not completed: %d (%.1f%%)", notCompleted,
            totalOrders > 0 ? (double) notCompleted / totalOrders * 100 : 0));
        
        System.out.println("\n   📊 SUCCESS METRICS:");
        System.out.println(String.format("      Completion rate: %.1f%% %s", 
            completionRate, getCompletionRatingIcon(completionRate)));
        System.out.println(String.format("      On-time delivery rate: %.1f%% %s", 
            onTimeRate, getOnTimeRatingIcon(onTimeRate)));
        
        // Calcular productos
        int totalProducts = allOrders.stream().mapToInt(PlannerOrder::getTotalQuantity).sum();
        int assignedProducts = solution.getPlannerShipments().stream()
            .mapToInt(PlannerShipment::getQuantity).sum();
        double productCompletionRate = totalProducts > 0 ? (double) assignedProducts / totalProducts * 100 : 0;
        
        System.out.println(String.format("      Product assignment rate: %.1f%% (%d/%d products)",
            productCompletionRate, assignedProducts, totalProducts));
        
        System.out.println("   " + "-".repeat(60));
    }
    
    private String getCompletionRatingIcon(double rate) {
        if (rate >= 95) return "EXCELLENT";
        if (rate >= 85) return "VERY GOOD";
        if (rate >= 75) return "GOOD";
        if (rate >= 60) return "ACCEPTABLE";
        if (rate >= 40) return "NEEDS IMPROVEMENT";
        return "CRITICAL";
    }
    
    private String getOnTimeRatingIcon(double rate) {
        if (rate >= 98) return "EXCELLENT";
        if (rate >= 90) return "VERY GOOD";
        if (rate >= 80) return "GOOD";
        if (rate >= 70) return "ACCEPTABLE";
        if (rate >= 50) return "NEEDS IMPROVEMENT";
        return "CRITICAL";
    }
    
    /**
     * Imprimir gráfico de convergencia del costo
     */
    private void printCostConvergenceGraph(List<Double> costHistory, double initialCost) {
        if (costHistory.size() < 2) return;
        
        System.out.println("\nCOST CONVERGENCE GRAPH:");
        System.out.println("   " + "-".repeat(60));
        
        // Tomar muestras (máximo 30 puntos para que quepa en pantalla)
        int samples = Math.min(30, costHistory.size());
        int step = Math.max(1, costHistory.size() / samples);
        
        double minCost = costHistory.stream().min(Double::compare).orElse(0.0);
        double maxCost = costHistory.stream().max(Double::compare).orElse(initialCost);
        double range = maxCost - minCost;
        
        if (range == 0) range = 1; // Evitar división por cero
        
        for (int i = 0; i < costHistory.size(); i += step) {
            double cost = costHistory.get(i);
            int barLength = (int) ((cost - minCost) / range * 40);
            
            StringBuilder bar = new StringBuilder("   ");
            for (int j = 0; j < barLength; j++) {
                bar.append("█");
            }
            
            System.out.println(String.format("%4d: %s %.2f", i, bar.toString(), cost));
        }
        
        System.out.println("   " + "-".repeat(60));
        System.out.println("   Min: " + String.format("%.2f", minCost) + " | Max: " + String.format("%.2f", maxCost));
    }
    
    /**
     * Imprimir reporte detallado por orden (LOGGING COMPLETO)
     */
    private void printDetailedOrderReport(TabuSolution solution) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("=== DETAILED ORDER AND ROUTE REPORT ===");
        System.out.println("=".repeat(80));
        
        List<PlannerOrder> allOrders = solution.getAllOrders();
        
        for (PlannerOrder order : allOrders) {
            List<PlannerShipment> orderShipments = solution.getShipmentsForOrder(order);
            int assignedQty = orderShipments.stream().mapToInt(PlannerShipment::getQuantity).sum();
            boolean isComplete = assignedQty >= order.getTotalQuantity();
            
            // Icono de estado de la orden
            String orderStatusIcon = "";
            if (isComplete) {
                boolean allOnTime = orderShipments.stream().allMatch(PlannerShipment::meetsDeadline);
                orderStatusIcon = allOnTime ? "✅" : "⚠️";
            } else if (assignedQty > 0) {
                orderStatusIcon = "⚠️";
            }
            
            System.out.println(String.format("\n%s ORDER #%d", orderStatusIcon, order.getId()));
            System.out.println(String.format("   Origin: %s → Destination: %s",
                order.getOrigin().getCode(), order.getDestination().getCode()));
            System.out.println(String.format("   Total quantity: %d products", order.getTotalQuantity()));
            System.out.println(String.format("   Max delivery time: %d hours (%s)",
                order.getMaxDeliveryHours(),
                order.isInterContinental() ? "intercontinental" : "same continent"));
            System.out.println(String.format("   Order time: %s", order.getOrderTime()));
            
            if (orderShipments.isEmpty()) {
                System.out.println("   NO SHIPMENTS ASSIGNED");
                continue;
            }
            
            System.out.println(String.format("   Assigned quantity: %d/%d %s %.1f%%",
                assignedQty, order.getTotalQuantity(),
                isComplete ? "✅" : (assignedQty > 0 ? "⚠️" : "❌"),
                (double) assignedQty / order.getTotalQuantity() * 100));
            System.out.println(String.format("   Number of shipments: %d", orderShipments.size()));
            
            // Detalles de cada shipment
            int shipmentNum = 1;
            for (PlannerShipment shipment : orderShipments) {
                String shipmentIcon = shipment.meetsDeadline() ? "✅" : "⚠️";
                System.out.println(String.format("\n   %s Shipment #%d (ID: %d):", shipmentIcon, shipmentNum++, shipment.getId()));
                System.out.println(String.format("      Quantity: %d products (%.1f%% of order)",
                    shipment.getQuantity(),
                    (double) shipment.getQuantity() / order.getTotalQuantity() * 100));
                System.out.println(String.format("      Route type: %s (%d stops)",
                    shipment.isDirect() ? "DIRECT" : "WITH CONNECTIONS",
                    shipment.getNumberOfStops()));
                System.out.println(String.format("      Route: %s", shipment.getRouteDescription()));
                
                if (!shipment.isDirect()) {
                    System.out.println(String.format("      Stopovers: %s",
                        String.join(", ", shipment.getStopoverAirports())));
                }
                
                System.out.println(String.format("      Flights: %s", shipment.getDetailedRouteDescription()));
                System.out.println(String.format("      Departure: %s", shipment.getInitialDepartureTime()));
                System.out.println(String.format("      Arrival: %s", shipment.getFinalArrivalTime()));
                System.out.println(String.format("      Travel time: %d hours", shipment.getTotalTravelHours()));
                System.out.println(String.format("      Delivery time: %d hours (max: %d) %s",
                    shipment.getDeliveryTimeHours(),
                    order.getMaxDeliveryHours(),
                    shipment.meetsDeadline() ? "✅ ON TIME" : "LATE"));
                System.out.println(String.format("      Valid sequence: %s",
                    shipment.isValidSequence() ? "✅ YES" : "NO"));
            }
        }
        
        System.out.println("\n" + "=".repeat(80));
    }
}
