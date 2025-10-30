# 📋 PLAN COMPLETO DE IMPLEMENTACIÓN - MoraPack K-Refactor

**Fecha:** 30 de Octubre, 2025  
**Proyecto:** MoraPack - Implementación del concepto K y correcciones críticas  
**Contexto:** Sistema de planificación de rutas aéreas con 3 escenarios (Operaciones Diarias, Simulación Semanal, Simulación Colapso)

---

## 📊 RESUMEN EJECUTIVO

Este documento unifica todos los análisis previos y presenta un plan completo de implementación que incluye:

1. **Corrección de problemas críticos** en el manejo de capacidades de aeropuertos
2. **Implementación del concepto K** para los 3 escenarios de operación
3. **Arquitectura DataProvider** (BD para Operaciones Diarias, Archivos para Simulaciones)
4. **STOMP/WebSocket** con topics individuales por simulación
5. **SimulationManager** para gestionar múltiples instancias concurrentes
6. **Persistencia en BD** SOLO para operaciones diarias (simulaciones solo streaming)
7. **Eliminación de código redundante** y optimización

### Estimaciones de Tiempo (ACTUALIZADO)

| Categoría | Sin IA | Con Cursor | Reducción |
|-----------|--------|------------|-----------|
| **CRÍTICO (Capacidades)** | 20-27.5h | 13-16.75h | ~40% |
| **CORE (K + DataProviders)** | 42-56h | 26-36h | ~40% |
| **WEBSOCKET/STOMP** | 14-19h | 8-11.5h | ~40% |
| **EVENTOS** | 12-23h | 7-11.5h | ~40% |
| **LIMPIEZA** | 6-9h | 3-5h | ~50% |
| **TOTAL** | **94-134.5h** | **57-81h** | **~40%** |

**Estimación realista con Cursor:** 57-81 horas (~7-10 días laborables)

### 🎯 Decisiones Arquitectónicas Clave

1. **Persistencia Diferenciada:**
   - ✅ Operaciones Diarias → BD completa (producción)
   - ❌ Simulaciones → Solo streaming (análisis temporal)

2. **Fuentes de Datos:**
   - Operaciones Diarias → `DatabaseDataProvider` (lee BD)
   - Simulaciones → `FileDataProvider` (lee CSV + expande on-the-fly)

3. **WebSocket con STOMP:**
   - Operaciones Diarias → Topic global `/topic/daily` (todos ven lo mismo)
   - Simulaciones → Topics individuales `/topic/sim/{id}` (cada usuario su simulación)

---

## 🔴 FASE 0: PROBLEMAS CRÍTICOS IDENTIFICADOS

### Problema 0.1: Capacidades de Aeropuerto - SOLO SOFT PENALTY ❌

**Severidad:** CRÍTICA  
**Descripción:** El algoritmo actual permite sobrecargas físicamente imposibles en los aeropuertos.

#### Código Actual Problemático

```java
// TabuSearchPlannerCostFunction.java, líneas 207-210
if (load > capacity) {
    int excess = load - capacity;
    penalty += AIRPORT_CAPACITY_VIOLATION_PENALTY;
    penalty += excess * AIRPORT_CAPACITY_UNIT_PENALTY;
}
// ⚠️ ESTO ES SOFT PENALTY, NO PREVIENE SOBRECARGAS REALES
```

#### Problema en Greedy Allocation

```java
// TabuSearchPlanner.java, línea 545
Map<PlannerFlight, Integer> flightCapacityRemaining = new HashMap<>();
// ❌ NO HAY: Map<PlannerAirport, Integer> airportCapacityRemaining

for (PlannerFlight flight : flights) {
    flightCapacityRemaining.put(flight, flight.getCapacity());
}
// Solo se trackea capacidad de vuelos, NO de aeropuertos
```

**Consecuencia:** El algoritmo puede generar planes donde un aeropuerto con capacidad 600 recibe 1500 productos simultáneamente.

---

### Problema 0.2: Falta de Validación en Fase de Mejora

En los movimientos (Split, Merge, Transfer, Reroute) del Tabu Search, no hay validación estricta de capacidad de aeropuerto antes de aceptar un movimiento.

---

### Problema 0.3: `PlannerAirport` existe pero no se usa completamente ✅

**Estado:** `PlannerAirport.java` YA está bien implementado con:
- ✅ `storageCapacity`
- ✅ `code`, `city`, `country`
- ✅ Método `getContinent()`
- ✅ Constructores y equals/hashCode

**Problema:** NO se está usando en el algoritmo Tabu actual.

### Problema 0.4: `AirportStorageManager` existe pero NO integrado

**Estado:** Ya existe `AirportStorageManager.java` con lógica de:
- Reservas de capacidad
- Tracking de ocupación
- Validaciones básicas

**Problema:** NO está integrado con el `TabuSearchPlanner`. El algoritmo NO lo usa.

**Solución:** Mejorar el `AirportStorageManager` existente (renombrar a `AirportCapacityManager` opcionalmente) e integrarlo en el Tabu Search.

---

## 🎯 FASE 1: SOLUCIÓN CRÍTICA - AIRPORT CAPACITY MANAGER

**Objetivo:** Implementar control estricto (hard constraint) de capacidades de aeropuerto.

### Tarea 1.1: Verificar y Mejorar `PlannerAirport` (YA EXISTE ✅)

**Tiempo estimado:** Sin IA: 0.5h | **Con Cursor: 0.25h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/entities/PlannerAirport.java` (EXISTENTE)

**Estado Actual:** ✅ Ya está bien implementado con:
```java
public class PlannerAirport {
    private int id;
    private String name;
    private String code;
    private String city;
    private Country country;
    private int storageCapacity;  // ✅ Ya existe!
    private int gmt;
    private double latitude;
    private double longitude;
    
    public Continent getContinent() {  // ✅ Ya existe!
        return country != null ? country.getContinent() : null;
    }
}
```

**Acción:** Solo verificar que tiene todo lo necesario. Opcionalmente añadir:

```java
// Opcional: método helper para identificar sedes principales
public boolean isMainHub() {
    // Lima, Bruselas, Baku
    return code.equals("SPIM") || code.equals("EBCI") || code.equals("UBBB");
}
```

**Con Cursor:** Trivial, solo añadir método helper si hace falta.

---

### Tarea 1.2: Mejorar `AirportStorageManager` existente (o crear `AirportCapacityManager`)

**Tiempo estimado:** Sin IA: 5-7h | **Con Cursor: 2.5-3.5h** ⚡

**Nota:** Ya existe `AirportStorageManager.java` con ~60% de lo necesario. Podemos mejorarlo o crear `AirportCapacityManager` desde cero.

**Recomendación:** Mejorar el existente.

**Archivo:** `pe/edu/pucp/morapack/algos/utils/AirportStorageManager.java` (MODIFICAR)  
O crear nuevo: `pe/edu/pucp/morapack/algos/algorithm/AirportCapacityManager.java`

```java
package pe.edu.pucp.morapack.algos.algorithm;

import java.util.*;

/**
 * Gestiona ESTRICTAMENTE las capacidades de almacenamiento de aeropuertos.
 * Implementa HARD CONSTRAINTS para prevenir sobrecargas físicamente imposibles.
 */
public class AirportCapacityManager {
    
    // Estado mutable: carga actual en cada aeropuerto
    private final Map<String, Integer> currentLoad;
    
    // Capacidades máximas (inmutables durante una ejecución)
    private final Map<String, Integer> maxCapacities;
    
    public AirportCapacityManager(List<PlannerAirport> airports) {
        this.currentLoad = new HashMap<>();
        this.maxCapacities = new HashMap<>();
        
        for (PlannerAirport airport : airports) {
            String code = airport.getCode();
            currentLoad.put(code, 0);
            maxCapacities.put(code, airport.getStorageCapacity());
        }
    }
    
    /**
     * Verifica si se puede añadir cantidad a un aeropuerto SIN exceder capacidad.
     */
    public boolean canAdd(String airportCode, int quantity) {
        int current = currentLoad.getOrDefault(airportCode, 0);
        int max = maxCapacities.getOrDefault(airportCode, Integer.MAX_VALUE);
        return (current + quantity) <= max;
    }
    
    /**
     * Añade cantidad a un aeropuerto. DEBE llamarse SOLO después de canAdd().
     * @throws IllegalStateException si excede capacidad
     */
    public void add(String airportCode, int quantity) {
        if (!canAdd(airportCode, quantity)) {
            throw new IllegalStateException(
                String.format("Cannot add %d to %s: exceeds capacity", 
                    quantity, airportCode)
            );
        }
        currentLoad.merge(airportCode, quantity, Integer::sum);
    }
    
    /**
     * Remueve cantidad de un aeropuerto (cuando los productos despegan).
     */
    public void remove(String airportCode, int quantity) {
        int current = currentLoad.getOrDefault(airportCode, 0);
        if (quantity > current) {
            throw new IllegalStateException(
                String.format("Cannot remove %d from %s: only %d present", 
                    quantity, airportCode, current)
            );
        }
        currentLoad.put(airportCode, current - quantity);
    }
    
    /**
     * Obtiene carga actual en un aeropuerto.
     */
    public int getCurrentLoad(String airportCode) {
        return currentLoad.getOrDefault(airportCode, 0);
    }
    
    /**
     * Obtiene capacidad disponible en un aeropuerto.
     */
    public int getAvailableCapacity(String airportCode) {
        int current = getCurrentLoad(airportCode);
        int max = maxCapacities.getOrDefault(airportCode, 0);
        return max - current;
    }
    
    /**
     * Clona el estado actual (útil para evaluar movimientos sin commitear).
     */
    public AirportCapacityManager clone() {
        AirportCapacityManager copy = new AirportCapacityManager(new ArrayList<>());
        copy.currentLoad.putAll(this.currentLoad);
        copy.maxCapacities.putAll(this.maxCapacities);
        return copy;
    }
    
    /**
     * Resetea las cargas actuales (útil entre ejecuciones).
     */
    public void reset() {
        for (String code : currentLoad.keySet()) {
            currentLoad.put(code, 0);
        }
    }
    
    /**
     * Obtiene snapshot del estado actual (para debugging).
     */
    public Map<String, String> getSnapshot() {
        Map<String, String> snapshot = new HashMap<>();
        for (String code : currentLoad.keySet()) {
            int current = currentLoad.get(code);
            int max = maxCapacities.get(code);
            snapshot.put(code, String.format("%d/%d", current, max));
        }
        return snapshot;
    }
}
```

**Con Cursor:** 
- Cursor puede generar métodos helper rápidamente
- Autocompletado inteligente para lógica similar
- Generación de JavaDoc automática

---

### Tarea 1.3: Integrar `AirportCapacityManager` en Greedy Allocation

**Tiempo estimado:** Sin IA: 4-6h | **Con Cursor: 2.5-3.5h** ⚡

**Archivo:** `TabuSearchPlanner.java`

**Cambios necesarios:**

```java
// ANTES (línea ~545)
Map<PlannerFlight, Integer> flightCapacityRemaining = new HashMap<>();
for (PlannerFlight flight : flights) {
    flightCapacityRemaining.put(flight, flight.getCapacity());
}

// DESPUÉS
Map<PlannerFlight, Integer> flightCapacityRemaining = new HashMap<>();
AirportCapacityManager airportCapacityManager = new AirportCapacityManager(airports);

for (PlannerFlight flight : flights) {
    flightCapacityRemaining.put(flight, flight.getCapacity());
}
```

**Modificar lógica de asignación:**

```java
// ANTES
for (Route route : possibleRoutes) {
    int toAssign = Math.min(remainingProducts, route.getMinCapacity());
    // ...
    updateCapacities(route.getFlights(), toAssign, flightCapacityRemaining);
}

// DESPUÉS
for (Route route : possibleRoutes) {
    int toAssign = Math.min(remainingProducts, route.getMinCapacity());
    
    // ✅ VERIFICAR CAPACIDAD DE CADA AEROPUERTO EN LA RUTA
    if (!canRouteAccommodate(route, toAssign, airportCapacityManager)) {
        continue; // Skip esta ruta, no hay capacidad
    }
    
    // Asignar y actualizar
    updateCapacities(route.getFlights(), toAssign, flightCapacityRemaining);
    updateAirportCapacities(route, toAssign, airportCapacityManager);
}

// NUEVO MÉTODO
private boolean canRouteAccommodate(
    Route route, 
    int quantity, 
    AirportCapacityManager manager
) {
    // Verificar cada aeropuerto intermedio (no el destino final)
    for (int i = 0; i < route.getFlights().size() - 1; i++) {
        PlannerFlight flight = route.getFlights().get(i);
        String destinationCode = flight.getDestinationCode();
        
        if (!manager.canAdd(destinationCode, quantity)) {
            return false;
        }
    }
    return true;
}

private void updateAirportCapacities(
    Route route, 
    int quantity, 
    AirportCapacityManager manager
) {
    // Añadir a aeropuertos intermedios
    for (int i = 0; i < route.getFlights().size() - 1; i++) {
        PlannerFlight flight = route.getFlights().get(i);
        manager.add(flight.getDestinationCode(), quantity);
    }
}
```

**Con Cursor:**
- Cursor puede ayudar a identificar todos los lugares donde se asignan productos
- Autocompletado de los nuevos métodos
- Refactoring asistido

---

### Tarea 1.4: Integrar en Movimientos Tabu (Split, Merge, Transfer, Reroute)

**Tiempo estimado:** Sin IA: 6-8h | **Con Cursor: 3.5-5h** ⚡

**Cambios en cada movimiento:**

```java
// EJEMPLO: Split Move
private SplitMoveResult evaluateSplitMove(PlannerShipment shipment, ...) {
    // ...
    
    // ✅ ANTES de aceptar el movimiento, validar capacidades
    AirportCapacityManager tempManager = currentAirportManager.clone();
    
    // Simular cambio
    removeShipmentFromAirports(shipment, tempManager);
    addShipmentToAirports(newShipment1, tempManager);
    addShipmentToAirports(newShipment2, tempManager);
    
    // Si alguno falla, rechazar el movimiento
    if (!allAddsSucceeded) {
        return null; // Movimiento inválido
    }
    
    // ...
}
```

**Aplicar patrón similar en:**
- `evaluateSplitMove()`
- `evaluateMergeMove()`
- `evaluateTransferMove()`
- `evaluateRerouteMove()`

**Con Cursor:**
- Patrón se puede replicar con Cursor de forma semi-automática
- "Apply this validation pattern to all move methods"

---

### Tarea 1.5: Actualizar `TabuSearchPlannerCostFunction`

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

**Cambio:** Convertir el soft penalty en un HARD CONSTRAINT detection.

```java
// ANTES
private double calculateAirportCapacityPenalty(...) {
    // ...
    if (load > capacity) {
        penalty += AIRPORT_CAPACITY_VIOLATION_PENALTY;
    }
    return penalty;
}

// DESPUÉS
private double calculateAirportCapacityPenalty(...) {
    // ...
    if (load > capacity) {
        // Esto NO debería ocurrir nunca si AirportCapacityManager funciona
        // Log CRITICAL error
        logger.error("CRITICAL: Airport {} overloaded: {}/{}", 
            airportCode, load, capacity);
        
        // Penalizar FUERTEMENTE para debugging
        penalty += AIRPORT_CAPACITY_VIOLATION_PENALTY * 1000;
    }
    return penalty;
}
```

**Además:** Añadir métricas de monitoreo.

---

### Tarea 1.6: Tests Unitarios para `AirportCapacityManager`

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 0.5-1h** ⚡

**Archivo:** `test/.../AirportCapacityManagerTest.java`

```java
@Test
void testCannotExceedCapacity() {
    AirportCapacityManager manager = ...;
    assertTrue(manager.canAdd("SPIM", 500));
    manager.add("SPIM", 500);
    
    assertFalse(manager.canAdd("SPIM", 400)); // Exceeds 800 capacity
}

@Test
void testRemoveProducts() {
    // ...
}

@Test
void testClone() {
    // ...
}
```

**Con Cursor:**
- Cursor puede generar casos de prueba automáticamente
- "Generate tests for AirportCapacityManager"
- Reducción de tiempo muy significativa (~75%)

---

### ✅ Resumen Fase 1: Capacidades de Aeropuerto

| Tarea | Sin IA | Con Cursor | Archivos |
|-------|--------|------------|----------|
| 1.1 Verificar PlannerAirport (ya existe) | 0.5h | 0.25h | Existente |
| 1.2 Mejorar AirportStorageManager | 5-7h | 2.5-3.5h | Existente (modificar) |
| 1.3 Integrar en Greedy | 4-6h | 2.5-3.5h | TabuSearchPlanner.java |
| 1.4 Integrar en Movimientos | 6-8h | 3.5-5h | TabuSearchPlanner.java |
| 1.5 Actualizar CostFunction | 2-3h | 1-1.5h | TabuSearchPlannerCostFunction.java |
| 1.6 Tests Unitarios | 2-3h | 0.5-1h | Test files |
| **TOTAL FASE 1** | **20-27.5h** | **13-16.75h** | **⚠️ CRÍTICO** |

---

## 🚀 FASE 2: IMPLEMENTACIÓN DEL CONCEPTO K + DATA PROVIDERS

**Objetivo:** Permitir que el sistema maneje 3 escenarios con diferentes horizontes de planificación y fuentes de datos.

### Contexto: ¿Qué es K?

**K** es el horizonte de planificación en **minutos**:

| Escenario | K | Fuente de Datos | Persistencia |
|-----------|---|-----------------|--------------|
| **Operaciones Diarias** | 1 min | BD (DatabaseDataProvider) | ✅ Guarda rutas |
| **Simulación Semanal** | 120 min | Archivos (FileDataProvider) | ❌ Solo streaming |
| **Simulación Colapso** | 60 min | Archivos (FileDataProvider) | ❌ Solo streaming |

**Conceptos clave:**
1. El algoritmo Tabu **NO cambia**. Lo que cambia es cuántos datos (pedidos futuros) se le alimentan.
2. **Operaciones Diarias:** Lee de BD (datos reales), guarda resultados en BD.
3. **Simulaciones:** Lee de CSV (datos sintéticos), solo hace streaming al frontend.

### Arquitectura de Fuentes de Datos

```
┌──────────────────────────────────────┐
│         DailyScheduler               │
│    (Operaciones en Producción)       │
└──────────┬───────────────────────────┘
           │
           ├─→ DatabaseDataProvider
           │     ├─→ Orders desde BD
           │     ├─→ Flights desde BD (expandidos)
           │     └─→ Airports desde BD
           │
           └─→ Guarda: routes, shipments en BD
           
           
┌──────────────────────────────────────┐
│   WeeklyScheduler / CollapseScheduler│
│      (Simulaciones de Análisis)      │
└──────────┬───────────────────────────┘
           │
           ├─→ FileDataProvider
           │     ├─→ FlightTemplate desde CSV
           │     │     └─→ FlightExpander (genera vuelos para cada día)
           │     ├─→ OrderTemplate desde CSV
           │     │     └─→ OrderDateInterpreter (asigna fechas absolutas)
           │     └─→ Airports desde TXT
           │
           └─→ NO guarda en BD, solo streaming via WebSocket
```

---

### 📦 SUBFASE 2.A: Data Providers (NUEVO - CRÍTICO)

**Objetivo:** Abstraer fuentes de datos para que schedulers funcionen con BD o archivos indistintamente.

---

### Tarea 2.A.1: Crear Interface `DataProvider`

**Tiempo estimado:** Sin IA: 1h | **Con Cursor: 0.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/DataProvider.java`

```java
package pe.edu.pucp/morapack.algos.scheduler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz para obtener datos del sistema.
 * Permite que los schedulers trabajen con diferentes fuentes (BD, archivos, etc.)
 */
public interface DataProvider {
    
    /**
     * Obtiene pedidos en un rango de tiempo
     */
    List<PlannerOrder> getOrders(LocalDateTime from, LocalDateTime to);
    
    /**
     * Obtiene vuelos disponibles en un rango de tiempo
     * (YA expandidos con fechas específicas)
     */
    List<PlannerFlight> getFlights(LocalDateTime from, LocalDateTime to);
    
    /**
     * Obtiene todos los aeropuertos del sistema
     */
    List<PlannerAirport> getAllAirports();
    
    /**
     * Obtiene bloqueos/cancelaciones en un rango de tiempo
     */
    List<Blockage> getBlockages(LocalDateTime from, LocalDateTime to);
}
```

---

### Tarea 2.A.2: Crear `FlightTemplate` y `FlightExpander`

**Tiempo estimado:** Sin IA: 3-4h | **Con Cursor: 2-2.5h** ⚡

**Archivos:**
- `pe/edu/pucp/morapack/algos/scheduler/FlightTemplate.java`
- `pe/edu/pucp/morapack/algos/scheduler/FlightExpander.java`

**Concepto:** Los vuelos en el CSV no tienen fechas específicas (solo hora: "03:34"). 
El `FlightExpander` genera vuelos para cada día del rango de simulación.

```java
/**
 * Representa un vuelo "plantilla" del CSV (sin fecha específica)
 */
public class FlightTemplate {
    private String origin;
    private String destination;
    private LocalTime departureTime;  // Solo hora: 03:34
    private LocalTime arrivalTime;    // Solo hora: 05:21
    private int capacity;
    
    // Constructor, getters...
}

/**
 * Expande vuelos template para todos los días de un rango
 */
public class FlightExpander {
    
    /**
     * Genera vuelos con fechas específicas para cada día
     * 
     * Ejemplo:
     *   Template: SKBO→SEQM @ 03:34 (sin fecha)
     *   Rango: 2025-01-06 a 2025-01-12
     *   
     *   Resultado:
     *     - PlannerFlight: SKBO→SEQM @ 2025-01-06 03:34
     *     - PlannerFlight: SKBO→SEQM @ 2025-01-07 03:34
     *     - PlannerFlight: SKBO→SEQM @ 2025-01-08 03:34
     *     ... (7 vuelos por template)
     */
    public List<PlannerFlight> expandFlights(
        List<FlightTemplate> templates,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        List<PlannerFlight> expandedFlights = new ArrayList<>();
        
        LocalDate currentDay = startDate.toLocalDate();
        LocalDate lastDay = endDate.toLocalDate();
        
        while (!currentDay.isAfter(lastDay)) {
            for (FlightTemplate template : templates) {
                LocalDateTime departure = currentDay.atTime(template.getDepartureTime());
                LocalDateTime arrival = calculateArrival(departure, template);
                
                PlannerFlight flight = new PlannerFlight(
                    generateFlightId(template, currentDay),
                    template.getOrigin(),
                    template.getDestination(),
                    departure,
                    arrival,
                    template.getCapacity()
                );
                
                expandedFlights.add(flight);
            }
            currentDay = currentDay.plusDays(1);
        }
        
        return expandedFlights;
    }
    
    private LocalDateTime calculateArrival(LocalDateTime departure, FlightTemplate template) {
        LocalTime arrivalTime = template.getArrivalTime();
        LocalTime departureTime = template.getDepartureTime();
        
        // Si llegada < salida → llega al día siguiente
        if (arrivalTime.isBefore(departureTime)) {
            return departure.toLocalDate().plusDays(1).atTime(arrivalTime);
        } else {
            return departure.toLocalDate().atTime(arrivalTime);
        }
    }
    
    private String generateFlightId(FlightTemplate template, LocalDate date) {
        return String.format("%s-%s-%s-%s",
            template.getOrigin(),
            template.getDestination(),
            date.toString(),
            template.getDepartureTime().toString()
        );
    }
}
```

**Con Cursor:** Lógica de expansión de fechas es repetitiva, Cursor ayuda mucho.

---

### Tarea 2.A.3: Crear `OrderTemplate` y `OrderDateInterpreter`

**Tiempo estimado:** Sin IA: 3-4h | **Con Cursor: 2-2.5h** ⚡

**Archivos:**
- `pe/edu/pucp/morapack/algos/scheduler/OrderTemplate.java`
- `pe/edu/pucp/morapack/algos/scheduler/OrderDateInterpreter.java`

**Concepto:** Los pedidos en el CSV tienen días relativos (dd=04), no fechas absolutas.
El `OrderDateInterpreter` convierte "día 04" → "2025-01-04 16:22" según el contexto.

```java
/**
 * Representa un pedido "plantilla" del CSV (día relativo)
 */
public class OrderTemplate {
    private int day;          // dd (01-31)
    private int hour;         // hh (00-23)
    private int minute;       // mm (00-59)
    private String destination;
    private int quantity;
    private String clientId;
    
    // Constructor, getters...
}

/**
 * Interpreta pedidos con días relativos a fechas absolutas
 */
public class OrderDateInterpreter {
    
    /**
     * Convierte pedidos template a pedidos con fechas absolutas
     * 
     * Ejemplo:
     *   Template: dd=07, hh=10, mm=30, dest=EDDI, qty=344
     *   Contexto: simulationStart = 2025-01-06, month=1, year=2025
     *   
     *   Resultado:
     *     PlannerOrder con orderTime = 2025-01-07 10:30
     */
    public List<PlannerOrder> interpretOrders(
        List<OrderTemplate> templates,
        LocalDateTime simulationStart,
        int month,
        int year
    ) {
        List<PlannerOrder> orders = new ArrayList<>();
        LocalDateTime simulationEnd = simulationStart.plusDays(7);
        
        for (OrderTemplate template : templates) {
            LocalDateTime orderTime = LocalDateTime.of(
                year,
                month,
                template.getDay(),
                template.getHour(),
                template.getMinute()
            );
            
            // Solo incluir pedidos dentro del rango de simulación
            if (isWithinRange(orderTime, simulationStart, simulationEnd)) {
                PlannerOrder order = new PlannerOrder(
                    generateOrderId(template),
                    template.getClientId(),
                    template.getDestination(),
                    template.getQuantity(),
                    orderTime
                );
                orders.add(order);
            }
        }
        
        return orders;
    }
    
    private boolean isWithinRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }
}
```

**Con Cursor:** Similar a FlightExpander, lógica repetitiva que Cursor acelera.

---

### Tarea 2.A.4: Crear `DatabaseDataProvider`

**Tiempo estimado:** Sin IA: 4-5h | **Con Cursor: 2.5-3h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/DatabaseDataProvider.java`

**Para:** Operaciones Diarias (lee datos reales de producción)

```java
@Service
@Transactional(readOnly = true)
public class DatabaseDataProvider implements DataProvider {
    
    private final OrderRepository orderRepository;
    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    
    private final OrderMapper orderMapper;
    private final FlightMapper flightMapper;
    private final AirportMapper airportMapper;
    
    @Override
    public List<PlannerOrder> getOrders(LocalDateTime from, LocalDateTime to) {
        List<Order> entities = orderRepository.findByOrderTimeBetween(from, to);
        return entities.stream()
            .map(orderMapper::toPlannerOrder)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlannerFlight> getFlights(LocalDateTime from, LocalDateTime to) {
        // ✅ Los vuelos YA están expandidos en BD con fechas específicas
        List<Flight> entities = flightRepository.findByDepartureTimeBetween(from, to);
        return entities.stream()
            .map(flightMapper::toPlannerFlight)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlannerAirport> getAllAirports() {
        List<Airport> entities = airportRepository.findAll();
        return entities.stream()
            .map(airportMapper::toPlannerAirport)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Blockage> getBlockages(LocalDateTime from, LocalDateTime to) {
        // TODO: Si hay tabla de bloqueos
        return new ArrayList<>();
    }
}
```

**Nota:** Requiere que la BD tenga vuelos expandidos (ver Tarea 2.A.6).

**Con Cursor:** Repositories y queries Spring Data JPA son muy estándar, Cursor los completa rápido.

---

### Tarea 2.A.5: Crear `FileDataProvider`

**Tiempo estimado:** Sin IA: 5-6h | **Con Cursor: 3-3.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/FileDataProvider.java`

**Para:** Simulaciones (lee datos sintéticos de archivos)

```java
@Service
public class FileDataProvider implements DataProvider {
    
    private final CsvDataLoader csvLoader;
    private final FlightExpander flightExpander;
    private final OrderDateInterpreter orderInterpreter;
    
    // Configuración de simulación (se establece al inicio)
    private LocalDateTime simulationStart;
    private int month;
    private int year;
    
    /**
     * Configura el provider para una simulación específica
     * (llamado desde WeeklyScheduler.start())
     */
    public void configureForSimulation(LocalDateTime start, int month, int year) {
        this.simulationStart = start;
        this.month = month;
        this.year = year;
    }
    
    @Override
    public List<PlannerOrder> getOrders(LocalDateTime from, LocalDateTime to) {
        // 1. Cargar templates del CSV
        List<OrderTemplate> templates = csvLoader.loadOrderTemplates("data/pedidos.csv");
        
        // 2. Interpretar con fechas absolutas
        List<PlannerOrder> allOrders = orderInterpreter.interpretOrders(
            templates, simulationStart, month, year
        );
        
        // 3. Filtrar por rango solicitado
        return allOrders.stream()
            .filter(o -> !o.getOrderTime().isBefore(from))
            .filter(o -> !o.getOrderTime().isAfter(to))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlannerFlight> getFlights(LocalDateTime from, LocalDateTime to) {
        // 1. Cargar templates del CSV
        List<FlightTemplate> templates = csvLoader.loadFlightTemplates("data/flights.csv");
        
        // 2. Expandir para toda la simulación
        LocalDateTime simulationEnd = simulationStart.plusDays(7);
        List<PlannerFlight> allFlights = flightExpander.expandFlights(
            templates, simulationStart, simulationEnd
        );
        
        // 3. Filtrar por rango solicitado
        return allFlights.stream()
            .filter(f -> !f.getDepartureTime().isBefore(from))
            .filter(f -> !f.getDepartureTime().isAfter(to))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlannerAirport> getAllAirports() {
        return csvLoader.loadAirports("data/airports.txt");
    }
    
    @Override
    public List<Blockage> getBlockages(LocalDateTime from, LocalDateTime to) {
        // Opcional: cargar cancelaciones programadas
        return csvLoader.loadCancellations("data/cancelaciones.txt", simulationStart);
    }
}
```

**Con Cursor:** File parsing repetitivo, Cursor ayuda bastante.

---

### Tarea 2.A.6: Actualizar `CsvDataLoader` para Templates

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/data/DataLoader.java` (MODIFICAR)

Añadir métodos para cargar templates:

```java
public class DataLoader {
    
    // ... métodos existentes ...
    
    /**
     * Carga templates de vuelos (sin fechas específicas)
     */
    public List<FlightTemplate> loadFlightTemplates(String filePath) {
        List<FlightTemplate> templates = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // Origen,Destino,HoraOrigen,HoraDestino,Capacidad
                FlightTemplate template = new FlightTemplate(
                    parts[0],  // origin
                    parts[1],  // destination
                    LocalTime.parse(parts[2]),  // departureTime
                    LocalTime.parse(parts[3]),  // arrivalTime
                    Integer.parseInt(parts[4])  // capacity
                );
                templates.add(template);
            }
        } catch (IOException e) {
            logger.error("Error loading flight templates", e);
        }
        
        return templates;
    }
    
    /**
     * Carga templates de pedidos (con días relativos)
     */
    public List<OrderTemplate> loadOrderTemplates(String filePath) {
        List<OrderTemplate> templates = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // dd,hh,mm,dest,###,IdClien
                OrderTemplate template = new OrderTemplate(
                    Integer.parseInt(parts[0]),  // day
                    Integer.parseInt(parts[1]),  // hour
                    Integer.parseInt(parts[2]),  // minute
                    parts[3],                     // destination
                    Integer.parseInt(parts[4]),  // quantity
                    parts[5]                      // clientId
                );
                templates.add(template);
            }
        } catch (IOException e) {
            logger.error("Error loading order templates", e);
        }
        
        return templates;
    }
}
```

**Con Cursor:** File parsing, muy rápido con IA.

---

### Tarea 2.A.7: Job para expandir vuelos en BD (Operaciones Diarias)

**Tiempo estimado:** Sin IA: 3-4h | **Con Cursor: 1.5-2h** ⚡

**Archivo:** `pe/edu/pucp/morapack/jobs/FlightExpansionJob.java` (NUEVO)

**Para:** Poblar BD con vuelos expandidos (necesario para DatabaseDataProvider)

```java
@Component
public class FlightExpansionJob {
    
    private final FlightRepository flightRepository;
    private final FlightExpander flightExpander;
    private final CsvDataLoader csvLoader;
    
    /**
     * Expande vuelos para los próximos 7 días cada día a medianoche
     */
    @Scheduled(cron = "0 0 0 * * *")  // Cada día a medianoche
    public void expandFlightsForNextWeek() {
        logger.info("Starting flight expansion job...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekLater = now.plusDays(7);
        
        // 1. Cargar templates desde CSV
        List<FlightTemplate> templates = csvLoader.loadFlightTemplates("data/flights.csv");
        
        // 2. Expandir para próximos 7 días
        List<PlannerFlight> plannerFlights = flightExpander.expandFlights(
            templates, now, weekLater
        );
        
        // 3. Convertir a entities
        List<Flight> entities = plannerFlights.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        
        // 4. Guardar en BD (evitar duplicados)
        List<Flight> saved = new ArrayList<>();
        for (Flight flight : entities) {
            if (!flightRepository.existsByDepartureTimeAndOriginAndDestination(
                flight.getDepartureTime(),
                flight.getOrigin(),
                flight.getDestination()
            )) {
                saved.add(flightRepository.save(flight));
            }
        }
        
        logger.info("Expanded {} flight templates into {} new flights",
            templates.size(), saved.size());
    }
    
    private Flight toEntity(PlannerFlight pf) {
        // Mapper logic
    }
}
```

**Con Cursor:** Jobs scheduled son muy estándar, Cursor los completa rápido.

---

### ✅ Resumen Subfase 2.A: Data Providers

| Tarea | Sin IA | Con Cursor | Archivos |
|-------|--------|------------|----------|
| 2.A.1 DataProvider interface | 1h | 0.5h | 1 nuevo |
| 2.A.2 FlightTemplate + Expander | 3-4h | 2-2.5h | 2 nuevos |
| 2.A.3 OrderTemplate + Interpreter | 3-4h | 2-2.5h | 2 nuevos |
| 2.A.4 DatabaseDataProvider | 4-5h | 2.5-3h | 1 nuevo |
| 2.A.5 FileDataProvider | 5-6h | 3-3.5h | 1 nuevo |
| 2.A.6 Actualizar CsvDataLoader | 2-3h | 1-1.5h | Modificar existente |
| 2.A.7 FlightExpansionJob (BD) | 3-4h | 1.5-2h | 1 nuevo |
| **TOTAL SUBFASE 2.A** | **21-29h** | **13-17.5h** | **8 nuevos + 1 modificado** |

---

### 📦 SUBFASE 2.B: Scheduler State y Config

---

### Tarea 2.1: Crear `SchedulerState`

**Tiempo estimado:** Sin IA: 4-6h | **Con Cursor: 2.5-3.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/SchedulerState.java`

```java
package pe.edu.pucp.morapack.algos.scheduler;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Estado MUTABLE del simulador/scheduler.
 * Representa el "mundo" en un momento dado: qué vuelos están activos,
 * qué pedidos están pendientes, qué productos están en tránsito, etc.
 */
public class SchedulerState {
    
    // ⏰ TIEMPO ACTUAL DE LA SIMULACIÓN
    private LocalDateTime currentTime;
    
    // 🛫 VUELOS DISPONIBLES (pueden estar en tierra o en vuelo)
    private List<PlannerFlight> activeFlights;
    
    // 📦 PEDIDOS PENDIENTES (aún no asignados o parcialmente asignados)
    private List<PlannerOrder> pendingOrders;
    
    // ✈️ ENVÍOS EN TRÁNSITO (productos asignados a rutas, en vuelo o en aeropuertos)
    private List<PlannerShipment> activeShipments;
    
    // 🚫 BLOQUEOS (vuelos cancelados, aeropuertos cerrados, etc.)
    private List<Blockage> blockages;
    
    // 🏢 AEROPUERTOS (con capacidades actuales)
    private AirportCapacityManager airportManager;
    
    // 🎯 PARÁMETROS DEL ESCENARIO
    private ScenarioConfig config;
    
    public SchedulerState(
        LocalDateTime startTime,
        List<PlannerAirport> airports,
        ScenarioConfig config
    ) {
        this.currentTime = startTime;
        this.activeFlights = new ArrayList<>();
        this.pendingOrders = new ArrayList<>();
        this.activeShipments = new ArrayList<>();
        this.blockages = new ArrayList<>();
        this.airportManager = new AirportCapacityManager(airports);
        this.config = config;
    }
    
    // ========================================
    // AVANCE DEL TIEMPO
    // ========================================
    
    /**
     * Avanza el reloj 1 minuto y actualiza el estado del mundo.
     */
    public void advanceOneMinute() {
        currentTime = currentTime.plusMinutes(1);
        
        // 1. Actualizar vuelos (despegar si es hora, aterrizar si llegaron)
        updateFlights();
        
        // 2. Actualizar envíos (mover productos entre aeropuertos/vuelos)
        updateShipments();
        
        // 3. Verificar deadlines de pedidos
        checkOrderDeadlines();
        
        // 4. Procesar blockages activos
        applyBlockages();
    }
    
    /**
     * Avanza el reloj N minutos.
     */
    public void advanceMinutes(int minutes) {
        for (int i = 0; i < minutes; i++) {
            advanceOneMinute();
        }
    }
    
    // ========================================
    // FILTRADO DE DATOS SEGÚN K
    // ========================================
    
    /**
     * Obtiene pedidos que están dentro del horizonte de planificación K.
     * Solo estos pedidos serán visibles para el algoritmo Tabu.
     */
    public List<PlannerOrder> getOrdersInPlanningHorizon() {
        int K = config.getMinutesToSimulate();
        LocalDateTime horizon = currentTime.plusMinutes(K);
        
        return pendingOrders.stream()
            .filter(order -> order.getOrderTime().isBefore(horizon))
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene vuelos que están disponibles en el horizonte K.
     */
    public List<PlannerFlight> getFlightsInPlanningHorizon() {
        int K = config.getMinutesToSimulate();
        LocalDateTime horizon = currentTime.plusMinutes(K);
        
        return activeFlights.stream()
            .filter(flight -> {
                LocalDateTime departure = flight.getDepartureTime();
                return departure.isAfter(currentTime) && departure.isBefore(horizon);
            })
            .collect(Collectors.toList());
    }
    
    // ========================================
    // ACTUALIZACIÓN DE ENTIDADES
    // ========================================
    
    private void updateFlights() {
        for (PlannerFlight flight : activeFlights) {
            if (flight.shouldDepart(currentTime)) {
                flight.depart();
                // Remover productos del aeropuerto de origen
                airportManager.remove(
                    flight.getOriginCode(), 
                    flight.getCurrentLoad()
                );
            }
            
            if (flight.shouldArrive(currentTime)) {
                flight.arrive();
                // Añadir productos al aeropuerto de destino
                airportManager.add(
                    flight.getDestinationCode(), 
                    flight.getCurrentLoad()
                );
            }
        }
    }
    
    private void updateShipments() {
        // Actualizar estado de cada envío según el tiempo actual
        for (PlannerShipment shipment : activeShipments) {
            shipment.updateStatus(currentTime);
        }
        
        // Remover envíos completados
        activeShipments.removeIf(s -> s.isCompleted());
    }
    
    private void checkOrderDeadlines() {
        // Marcar pedidos vencidos
        for (PlannerOrder order : pendingOrders) {
            if (order.isOverdue(currentTime)) {
                order.markAsOverdue();
            }
        }
    }
    
    private void applyBlockages() {
        // Cancelar vuelos, cerrar aeropuertos, etc.
        for (Blockage blockage : blockages) {
            if (blockage.isActive(currentTime)) {
                blockage.apply(this);
            }
        }
    }
    
    // ========================================
    // GESTIÓN DE PEDIDOS Y ENVÍOS
    // ========================================
    
    public void addOrder(PlannerOrder order) {
        pendingOrders.add(order);
    }
    
    public void removeOrder(PlannerOrder order) {
        pendingOrders.remove(order);
    }
    
    public void addShipment(PlannerShipment shipment) {
        activeShipments.add(shipment);
    }
    
    public void addBlockage(Blockage blockage) {
        blockages.add(blockage);
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public LocalDateTime getCurrentTime() {
        return currentTime;
    }
    
    public List<PlannerOrder> getPendingOrders() {
        return new ArrayList<>(pendingOrders);
    }
    
    public List<PlannerShipment> getActiveShipments() {
        return new ArrayList<>(activeShipments);
    }
    
    public AirportCapacityManager getAirportManager() {
        return airportManager;
    }
    
    public ScenarioConfig getConfig() {
        return config;
    }
}
```

**Con Cursor:**
- Estructura de clases grandes se beneficia de generación automática
- Cursor puede sugerir métodos helper basados en el contexto
- Reducción significativa en boilerplate

---

### Tarea 2.2: Crear `ScenarioConfig`

**Tiempo estimado:** Sin IA: 1-2h | **Con Cursor: 0.5-1h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/ScenarioConfig.java`

```java
package pe.edu.pucp.morapack.algos.scheduler;

/**
 * Configuración de un escenario de simulación/operación.
 */
public class ScenarioConfig {
    
    public enum ScenarioType {
        DAILY_OPERATIONS,
        WEEKLY_SIMULATION,
        COLLAPSE_SIMULATION
    }
    
    private ScenarioType type;
    private int minutesToSimulate;     // K
    private int msPerMinute;            // Velocidad de simulación
    private int replanningIntervalMinutes; // Cada cuánto re-planificar
    
    // ========================================
    // FACTORY METHODS PARA CADA ESCENARIO
    // ========================================
    
    public static ScenarioConfig dailyOperations() {
        ScenarioConfig config = new ScenarioConfig();
        config.type = ScenarioType.DAILY_OPERATIONS;
        config.minutesToSimulate = 1;           // K = 1 minuto
        config.msPerMinute = 60_000;             // 1 minuto real = 1 minuto simulado
        config.replanningIntervalMinutes = 1;    // Re-planificar cada minuto
        return config;
    }
    
    public static ScenarioConfig weeklySimulation() {
        ScenarioConfig config = new ScenarioConfig();
        config.type = ScenarioType.WEEKLY_SIMULATION;
        config.minutesToSimulate = 120;          // K = 2 horas
        config.msPerMinute = 200;                // 200ms real = 1 minuto simulado
        config.replanningIntervalMinutes = 120;  // Re-planificar cada 2 horas
        return config;
    }
    
    public static ScenarioConfig collapseSimulation() {
        ScenarioConfig config = new ScenarioConfig();
        config.type = ScenarioType.COLLAPSE_SIMULATION;
        config.minutesToSimulate = 60;           // K = 1 hora
        config.msPerMinute = 200;                // 200ms real = 1 minuto simulado
        config.replanningIntervalMinutes = 60;   // Re-planificar cada hora
        return config;
    }
    
    // Getters
    public ScenarioType getType() { return type; }
    public int getMinutesToSimulate() { return minutesToSimulate; }
    public int getMsPerMinute() { return msPerMinute; }
    public int getReplanningIntervalMinutes() { return replanningIntervalMinutes; }
}
```

**Con Cursor:**
- Clase simple, generación casi instantánea
- Cursor puede sugerir validaciones automáticamente

---

### Tarea 2.3: Crear Interfaces de Scheduler

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/Scheduler.java`

```java
package pe.edu.pucp.morapack.algos.scheduler;

/**
 * Interfaz base para todos los schedulers.
 */
public interface Scheduler {
    
    /**
     * Inicia el scheduler.
     */
    void start();
    
    /**
     * Detiene el scheduler.
     */
    void stop();
    
    /**
     * Pausa el scheduler (solo simulaciones).
     */
    void pause();
    
    /**
     * Reanuda el scheduler (solo simulaciones).
     */
    void resume();
    
    /**
     * Obtiene el estado actual.
     */
    SchedulerState getState();
}
```

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/DataProvider.java`

```java
package pe.edu.pucp.morapack.algos.scheduler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz para obtener datos del sistema (BD, archivos, etc.).
 */
public interface DataProvider {
    
    List<PlannerOrder> getOrders(LocalDateTime from, LocalDateTime to);
    
    List<PlannerFlight> getFlights(LocalDateTime from, LocalDateTime to);
    
    List<PlannerAirport> getAllAirports();
    
    List<Blockage> getBlockages(LocalDateTime from, LocalDateTime to);
}
```

**Con Cursor:**
- Interfaces son triviales de generar con IA

---

### Tarea 2.4: Implementar `DailyScheduler`

**Tiempo estimado:** Sin IA: 6-8h | **Con Cursor: 3-4h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/DailyScheduler.java`

```java
package pe.edu.pucp.morapack.algos.scheduler;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler para operaciones día a día (tiempo real).
 * K = 1 minuto.
 */
@Component
public class DailyScheduler implements Scheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(DailyScheduler.class);
    
    private final DataProvider dataProvider;
    private final TabuSearchPlanner tabuPlanner;
    private final WebSocketService webSocketService;
    
    private SchedulerState state;
    private volatile boolean running;
    
    public DailyScheduler(
        DataProvider dataProvider,
        TabuSearchPlanner tabuPlanner,
        WebSocketService webSocketService
    ) {
        this.dataProvider = dataProvider;
        this.tabuPlanner = tabuPlanner;
        this.webSocketService = webSocketService;
    }
    
    @Override
    public void start() {
        logger.info("Starting DailyScheduler...");
        
        // Configurar escenario
        ScenarioConfig config = ScenarioConfig.dailyOperations();
        
        // Cargar aeropuertos
        List<PlannerAirport> airports = dataProvider.getAllAirports();
        
        // Inicializar estado
        state = new SchedulerState(LocalDateTime.now(), airports, config);
        
        running = true;
        
        // Iniciar loop principal
        new Thread(this::mainLoop).start();
    }
    
    private void mainLoop() {
        while (running) {
            try {
                // 1. Obtener pedidos del último minuto
                LocalDateTime now = state.getCurrentTime();
                LocalDateTime nextMinute = now.plusMinutes(1);
                
                List<PlannerOrder> newOrders = dataProvider.getOrders(now, nextMinute);
                newOrders.forEach(state::addOrder);
                
                logger.info("Loaded {} new orders for minute {}", newOrders.size(), now);
                
                // 2. Ejecutar algoritmo Tabu con horizonte K=1
                List<PlannerOrder> ordersToProcess = state.getOrdersInPlanningHorizon();
                List<PlannerFlight> availableFlights = state.getFlightsInPlanningHorizon();
                
                if (!ordersToProcess.isEmpty()) {
                    logger.info("Planning for {} orders with {} flights", 
                        ordersToProcess.size(), availableFlights.size());
                    
                    List<PlannerShipment> newShipments = tabuPlanner.plan(
                        ordersToProcess,
                        availableFlights,
                        state.getAirportManager()
                    );
                    
                    // 3. Actualizar estado con nuevos envíos
                    newShipments.forEach(state::addShipment);
                    
                    // 4. Remover pedidos asignados
                    for (PlannerShipment shipment : newShipments) {
                        state.removeOrder(shipment.getOrder());
                    }
                    
                    // 5. Enviar actualización al frontend
                    webSocketService.sendUpdate(state);
                }
                
                // 6. Avanzar 1 minuto
                state.advanceOneMinute();
                
                // 7. Esperar 60 segundos (tiempo real)
                Thread.sleep(60_000);
                
            } catch (InterruptedException e) {
                logger.info("DailyScheduler interrupted");
                running = false;
            } catch (Exception e) {
                logger.error("Error in DailyScheduler loop", e);
            }
        }
    }
    
    @Override
    public void stop() {
        running = false;
        logger.info("DailyScheduler stopped");
    }
    
    @Override
    public void pause() {
        // No aplicable en operaciones diarias
    }
    
    @Override
    public void resume() {
        // No aplicable en operaciones diarias
    }
    
    @Override
    public SchedulerState getState() {
        return state;
    }
}
```

**Con Cursor:**
- Cursor puede ayudar con el patrón de threading
- Autocompletado de llamadas a métodos de `SchedulerState`
- Manejo de excepciones sugerido automáticamente

---

### Tarea 2.5: Implementar `WeeklyScheduler`

**Tiempo estimado:** Sin IA: 6-8h | **Con Cursor: 3-4h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/WeeklyScheduler.java`

```java
package pe.edu.pucp.morapack.algos.scheduler;

import org.springframework.stereotype.Component;

/**
 * Scheduler para simulación semanal.
 * K = 120 minutos (2 horas).
 * Velocidad: 200ms por minuto simulado.
 */
@Component
public class WeeklyScheduler implements Scheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(WeeklyScheduler.class);
    
    private final DataProvider dataProvider;
    private final TabuSearchPlanner tabuPlanner;
    private final WebSocketService webSocketService;
    
    private SchedulerState state;
    private volatile boolean running;
    private volatile boolean paused;
    
    // Similar a DailyScheduler pero con diferencias:
    
    @Override
    public void start() {
        logger.info("Starting WeeklyScheduler...");
        
        ScenarioConfig config = ScenarioConfig.weeklySimulation();
        List<PlannerAirport> airports = dataProvider.getAllAirports();
        
        // Simular desde un tiempo específico (e.g., inicio de semana)
        LocalDateTime simulationStart = LocalDateTime.of(2025, 1, 6, 0, 0);
        state = new SchedulerState(simulationStart, airports, config);
        
        // CARGAR TODOS LOS PEDIDOS DE LA SEMANA de una vez
        LocalDateTime weekEnd = simulationStart.plusDays(7);
        List<PlannerOrder> allOrders = dataProvider.getOrders(simulationStart, weekEnd);
        allOrders.forEach(state::addOrder);
        
        logger.info("Loaded {} orders for the week", allOrders.size());
        
        // Cargar todos los vuelos de la semana
        List<PlannerFlight> allFlights = dataProvider.getFlights(simulationStart, weekEnd);
        state.addAllFlights(allFlights);
        
        logger.info("Loaded {} flights for the week", allFlights.size());
        
        running = true;
        paused = false;
        
        new Thread(this::mainLoop).start();
    }
    
    private void mainLoop() {
        int minutesElapsed = 0;
        
        while (running && minutesElapsed < 7 * 24 * 60) { // 1 semana
            if (paused) {
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            try {
                // Re-planificar cada 120 minutos (K)
                if (minutesElapsed % 120 == 0) {
                    logger.info("Re-planning at minute {}", minutesElapsed);
                    
                    List<PlannerOrder> ordersToProcess = state.getOrdersInPlanningHorizon();
                    List<PlannerFlight> availableFlights = state.getFlightsInPlanningHorizon();
                    
                    if (!ordersToProcess.isEmpty()) {
                        List<PlannerShipment> newShipments = tabuPlanner.plan(
                            ordersToProcess,
                            availableFlights,
                            state.getAirportManager()
                        );
                        
                        newShipments.forEach(state::addShipment);
                        
                        for (PlannerShipment shipment : newShipments) {
                            state.removeOrder(shipment.getOrder());
                        }
                    }
                }
                
                // Avanzar 1 minuto simulado
                state.advanceOneMinute();
                minutesElapsed++;
                
                // Enviar actualización cada N minutos
                if (minutesElapsed % 10 == 0) {
                    webSocketService.sendUpdate(state);
                }
                
                // Esperar 200ms (tiempo real)
                Thread.sleep(200);
                
            } catch (InterruptedException e) {
                logger.info("WeeklyScheduler interrupted");
                running = false;
            } catch (Exception e) {
                logger.error("Error in WeeklyScheduler loop", e);
            }
        }
        
        logger.info("WeeklyScheduler completed. Total minutes: {}", minutesElapsed);
    }
    
    @Override
    public void pause() {
        paused = true;
        logger.info("WeeklyScheduler paused");
    }
    
    @Override
    public void resume() {
        paused = false;
        logger.info("WeeklyScheduler resumed");
    }
    
    // ... resto similar
}
```

**Con Cursor:**
- Copiar y adaptar desde `DailyScheduler` es rápido con IA
- Cursor sugiere los cambios necesarios (K, msPerMinute, etc.)

---

### Tarea 2.6: Implementar `CollapseScheduler`

**Tiempo estimado:** Sin IA: 6-8h | **Con Cursor: 3-4h** ⚡

**Muy similar a `WeeklyScheduler` pero:**
- K = 60 minutos
- Continúa hasta que el sistema colapse (no puede atender más pedidos)

```java
private void mainLoop() {
    int minutesElapsed = 0;
    int consecutiveUnassignedCycles = 0;
    
    while (running) {
        // ...
        
        // Detectar colapso
        if (ordersToProcess.size() > 0 && newShipments.isEmpty()) {
            consecutiveUnassignedCycles++;
            
            if (consecutiveUnassignedCycles >= 5) {
                logger.error("COLLAPSE DETECTED: Cannot assign orders for 5 consecutive cycles");
                running = false;
                break;
            }
        } else {
            consecutiveUnassignedCycles = 0;
        }
        
        // ...
    }
}
```

---

### Tarea 2.7: Crear `DatabaseDataProvider`

**Tiempo estimado:** Sin IA: 4-5h | **Con Cursor: 2-2.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/DatabaseDataProvider.java`

```java
package pe.edu.pucp.morapack.algos.scheduler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DatabaseDataProvider implements DataProvider {
    
    private final OrderRepository orderRepository;
    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final OrderMapper orderMapper;
    private final FlightMapper flightMapper;
    private final AirportMapper airportMapper;
    
    public DatabaseDataProvider(
        OrderRepository orderRepository,
        FlightRepository flightRepository,
        AirportRepository airportRepository,
        OrderMapper orderMapper,
        FlightMapper flightMapper,
        AirportMapper airportMapper
    ) {
        this.orderRepository = orderRepository;
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
        this.orderMapper = orderMapper;
        this.flightMapper = flightMapper;
        this.airportMapper = airportMapper;
    }
    
    @Override
    public List<PlannerOrder> getOrders(LocalDateTime from, LocalDateTime to) {
        List<Order> entities = orderRepository.findByOrderTimeBetween(from, to);
        return entities.stream()
            .map(orderMapper::toPlannerOrder)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlannerFlight> getFlights(LocalDateTime from, LocalDateTime to) {
        List<Flight> entities = flightRepository.findByDepartureTimeBetween(from, to);
        return entities.stream()
            .map(flightMapper::toPlannerFlight)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlannerAirport> getAllAirports() {
        List<Airport> entities = airportRepository.findAll();
        return entities.stream()
            .map(airportMapper::toPlannerAirport)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Blockage> getBlockages(LocalDateTime from, LocalDateTime to) {
        // Implementar si se tienen bloqueos en BD
        return new ArrayList<>();
    }
}
```

**Con Cursor:**
- Patrón Repository + Mapper es muy común, Cursor puede generarlo casi completamente
- Queries de Spring Data JPA son sugeridas automáticamente

---

### Tarea 2.8: Crear Mappers (Entity ↔ Planner)

**Tiempo estimado:** Sin IA: 4-5h | **Con Cursor: 1-1.5h** ⚡

**Ejemplo:** `OrderMapper.java`

```java
@Component
public class OrderMapper {
    
    public PlannerOrder toPlannerOrder(Order entity) {
        PlannerOrder planner = new PlannerOrder();
        planner.setId(entity.getId());
        planner.setClientId(entity.getClientId());
        planner.setDestinationCode(entity.getDestinationAirport().getCode());
        planner.setQuantity(entity.getQuantity());
        planner.setOrderTime(entity.getOrderTime());
        planner.setDeadline(calculateDeadline(entity));
        return planner;
    }
    
    public Order toEntity(PlannerOrder planner) {
        Order entity = new Order();
        entity.setId(planner.getId());
        // ...
        return entity;
    }
    
    private LocalDateTime calculateDeadline(Order entity) {
        // Lógica según continente: 2 días mismo, 3 días diferente
        // ...
    }
}
```

**Con Cursor:**
- Mappers son casi 100% automáticos con Cursor
- "Generate mapper methods for all fields"
- Reducción de tiempo ~75%

---

### Tarea 2.9: Actualizar `TabuSearchPlanner` para recibir `AirportCapacityManager`

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

```java
// ANTES
public List<PlannerShipment> plan(
    List<PlannerOrder> orders,
    List<PlannerFlight> flights
) {
    // ...
}

// DESPUÉS
public List<PlannerShipment> plan(
    List<PlannerOrder> orders,
    List<PlannerFlight> flights,
    AirportCapacityManager airportManager  // ✅ NUEVO
) {
    // El airportManager se pasa a todas las funciones internas
    // que evalúan asignaciones
    // ...
}
```

---

### Tarea 2.10: Tests de Integración para Schedulers

**Tiempo estimado:** Sin IA: 4-6h | **Con Cursor: 1.5-2.5h** ⚡

```java
@SpringBootTest
class DailySchedulerIntegrationTest {
    
    @Autowired
    private DailyScheduler scheduler;
    
    @Test
    void testSchedulerStartsAndProcessesOrders() {
        // Mock DataProvider
        // ...
        
        scheduler.start();
        
        // Wait 2 minutes
        Thread.sleep(120_000);
        
        SchedulerState state = scheduler.getState();
        assertThat(state.getActiveShipments()).isNotEmpty();
        
        scheduler.stop();
    }
}
```

**Con Cursor:**
- Tests de integración requieren setup, Cursor ayuda enormemente
- Generación de mocks y asserts automática

---

### ✅ Resumen Fase 2: Concepto K + DataProviders

**Subfase 2.A: Data Providers (13-17.5h con Cursor)**
| Tarea | Sin IA | Con Cursor |
|-------|--------|------------|
| 2.A.1 DataProvider interface | 1h | 0.5h |
| 2.A.2 FlightTemplate + Expander | 3-4h | 2-2.5h |
| 2.A.3 OrderTemplate + Interpreter | 3-4h | 2-2.5h |
| 2.A.4 DatabaseDataProvider | 4-5h | 2.5-3h |
| 2.A.5 FileDataProvider | 5-6h | 3-3.5h |
| 2.A.6 Actualizar CsvDataLoader | 2-3h | 1-1.5h |
| 2.A.7 FlightExpansionJob (BD) | 3-4h | 1.5-2h |

**Subfase 2.B: Schedulers + State (~9-14.5h con Cursor)**
| Tarea | Sin IA | Con Cursor |
|-------|--------|------------|
| 2.B.1 SchedulerState | 4-6h | 2.5-3.5h |
| 2.B.2 ScenarioConfig | 1-2h | 0.5-1h |
| 2.B.3 Scheduler interface | 1-2h | 0.5-1h |
| 2.B.4 DailyScheduler | 6-8h | 3-4h |
| 2.B.5 WeeklyScheduler | 6-8h | 3-4h |
| 2.B.6 CollapseScheduler | 6-8h | 3-4h |
| 2.B.7 Mappers (Entity ↔ Planner) | 4-5h | 1-1.5h |
| 2.B.8 Actualizar Tabu (recibir AirportManager) | 2-3h | 1-1.5h |
| 2.B.9 Tests Integración | 4-6h | 1.5-2.5h |

| **TOTAL FASE 2** | **42-56h** | **26-36h** | **⚠️ CORE** |

---

## 🎮 FASE 3: WEBSOCKET/STOMP + EVENTOS DINÁMICOS

**Objetivo:** 
1. Configurar WebSocket/STOMP para comunicación en tiempo real
2. Implementar SimulationManager para gestionar múltiples simulaciones concurrentes
3. Permitir que el sistema reaccione a eventos dinámicos (nuevos pedidos, cancelaciones)

### 🌐 Arquitectura WebSocket/STOMP

**Diferencia clave:**
- **Operaciones Diarias:** 1 instancia (singleton) → Topic global `/topic/daily` → TODOS ven lo mismo
- **Simulaciones:** N instancias → Topics individuales `/topic/sim/{id}` → CADA UNO ve la suya

```
┌─────────────────────────────────────────┐
│       Operaciones Diarias               │
│  (1 instancia singleton)                │
└───────────┬─────────────────────────────┘
            │
            ├─→ /topic/daily (GLOBAL)
            │
     ┌──────┴───────┬──────────┬─────────┐
     ▼              ▼          ▼         ▼
┌─────────┐  ┌─────────┐  ┌─────────┐  ...
│ User 1  │  │ User 2  │  │ User 3  │
│ (watch) │  │ (watch) │  │ (watch) │
└─────────┘  └─────────┘  └─────────┘
    Todos ven: mismos pedidos, mismos vuelos


┌─────────────────────────────────────────┐
│          Simulaciones                   │
│  (N instancias, 1 por usuario)          │
└───────────┬─────────────────────────────┘
            │
     ┌──────┴──────────┬──────────────┐
     │                 │              │
     ▼                 ▼              ▼
/topic/sim/abc   /topic/sim/xyz   /topic/sim/def
     │                 │              │
     ▼                 ▼              ▼
┌─────────┐      ┌─────────┐    ┌─────────┐
│ User 1  │      │ User 2  │    │ User 3  │
│(ejecuta)│      │(ejecuta)│    │(ejecuta)│
└─────────┘      └─────────┘    └─────────┘
  Sim #1           Sim #2         Sim #3
```

---

### 📦 SUBFASE 3.A: WebSocket/STOMP Integration

---

### Tarea 3.A.1: Crear `SimulationManager`

**Tiempo estimado:** Sin IA: 4-5h | **Con Cursor: 2-2.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/SimulationManager.java`

**Objetivo:** Gestionar múltiples simulaciones concurrentes (cada usuario ejecuta la suya).

```java
@Service
public class SimulationManager {
    
    // Mapa de simulaciones activas
    private final ConcurrentHashMap<String, Scheduler> activeSimulations;
    
    private final WeeklySchedulerFactory weeklyFactory;
    private final CollapseSchedulerFactory collapseFactory;
    
    public SimulationManager() {
        this.activeSimulations = new ConcurrentHashMap<>();
    }
    
    /**
     * Inicia una simulación semanal para un usuario
     */
    public String startWeeklySimulation(WeeklySimulationRequest request) {
        // 1. Crear instancia nueva de scheduler
        WeeklyScheduler scheduler = weeklyFactory.create();
        
        // 2. Iniciar simulación (devuelve ID único)
        String simulationId = scheduler.start(request);
        
        // 3. Registrar en mapa
        activeSimulations.put(simulationId, scheduler);
        
        // 4. Auto-cleanup cuando termine
        scheduler.onComplete(() -> {
            activeSimulations.remove(simulationId);
            logger.info("Simulation {} completed and removed", simulationId);
        });
        
        logger.info("Started simulation {}. Total active: {}", 
            simulationId, activeSimulations.size());
        
        return simulationId;
    }
    
    /**
     * Para una simulación específica
     */
    public void stopSimulation(String simulationId) {
        Scheduler scheduler = activeSimulations.get(simulationId);
        if (scheduler != null) {
            scheduler.stop();
            activeSimulations.remove(simulationId);
        }
    }
    
    public void pauseSimulation(String simulationId) {
        Scheduler scheduler = activeSimulations.get(simulationId);
        if (scheduler != null) {
            scheduler.pause();
        }
    }
    
    public void resumeSimulation(String simulationId) {
        Scheduler scheduler = activeSimulations.get(simulationId);
        if (scheduler != null) {
            scheduler.resume();
        }
    }
    
    /**
     * Obtiene lista de simulaciones activas (para admin)
     */
    public List<SimulationInfo> getActiveSimulations() {
        return activeSimulations.entrySet().stream()
            .map(e -> new SimulationInfo(
                e.getKey(),
                e.getValue().getType(),
                e.getValue().getState().getCurrentTime()
            ))
            .collect(Collectors.toList());
    }
}
```

**Con Cursor:** Gestión de maps concurrentes es estándar, Cursor ayuda con thread-safety.

---

### Tarea 3.A.2: Crear Factories para Schedulers

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

**Archivos:**
- `pe/edu/pucp/morapack/algos/scheduler/WeeklySchedulerFactory.java`
- `pe/edu/pucp/morapack/algos/scheduler/CollapseSchedulerFactory.java`

```java
@Service
public class WeeklySchedulerFactory {
    
    private final FileDataProvider dataProvider;
    private final TabuSearchPlanner tabuPlanner;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Crea una NUEVA instancia de WeeklyScheduler
     * (no singleton, cada llamada = nueva instancia)
     */
    public WeeklyScheduler create() {
        return new WeeklyScheduler(
            dataProvider,
            tabuPlanner,
            messagingTemplate
        );
    }
}
```

**Con Cursor:** Factories simples, generación rápida.

---

### Tarea 3.A.3: Actualizar Schedulers para usar STOMP Topics

**Tiempo estimado:** Sin IA: 3-4h | **Con Cursor: 1.5-2.5h** ⚡

**Modificar:** 
- `DailyScheduler.java` → Topic: `/topic/daily`
- `WeeklyScheduler.java` → Topic: `/topic/sim/{simulationId}`
- `CollapseScheduler.java` → Topic: `/topic/sim/{simulationId}`

```java
// DailyScheduler.java
@Service
public class DailyScheduler {
    
    private final SimpMessagingTemplate messagingTemplate;
    private static final String DAILY_TOPIC = "/topic/daily";  // ✅ GLOBAL
    
    private void mainLoop() {
        while (running) {
            // ... planificar ...
            
            // ✅ BROADCAST a TODOS
            StateUpdateMessage update = buildUpdate(state);
            messagingTemplate.convertAndSend(DAILY_TOPIC, update);
            
            Thread.sleep(60_000);
        }
    }
}

// WeeklyScheduler.java
public class WeeklyScheduler {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    private String simulationId;
    private String topicDestination;  // ✅ Topic individual
    
    public String start(WeeklySimulationRequest request) {
        // Generar ID único
        this.simulationId = UUID.randomUUID().toString();
        this.topicDestination = "/topic/sim/" + simulationId;
        
        // Iniciar thread
        new Thread(this::mainLoop).start();
        
        return simulationId;
    }
    
    private void mainLoop() {
        while (running) {
            // ... planificar ...
            
            // ✅ ENVIAR solo a SU topic
            StateUpdateMessage update = buildUpdate(state);
            messagingTemplate.convertAndSend(topicDestination, update);
            
            Thread.sleep(200);
        }
    }
}
```

**Con Cursor:** Cambios simples de topic strings, rápido con IA.

---

### Tarea 3.A.4: Crear DTOs para WebSocket

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

**Archivos:** `pe/edu/pucp/morapack/dto/websocket/`

```java
@Data
public class StateUpdateMessage {
    private String type = "STATE_UPDATE";
    private String simulationId;
    private LocalDateTime currentTime;
    private List<ShipmentDto> activeShipments;
    private List<OrderDto> pendingOrders;
    private MetricsDto metrics;
}

@Data
public class SimulationCompleteMessage {
    private String type = "SIMULATION_COMPLETE";
    private String simulationId;
    private MetricsDto finalMetrics;
    private int totalMinutes;
}

@Data
public class MetricsDto {
    private int totalOrders;
    private int ordersDelivered;
    private int ordersLate;
    private double avgDeliveryTime;
    private double flightUtilization;
}
```

**Con Cursor:** DTOs son boilerplate puro, Cursor los genera instantáneamente.

---

### Tarea 3.A.5: Actualizar `SimulationController` con endpoints completos

**Tiempo estimado:** Sin IA: 3-4h | **Con Cursor: 1.5-2.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/controller/SimulationController.java` (MODIFICAR)

```java
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {
    
    private final SimulationManager simulationManager;
    
    @PostMapping("/weekly/start")
    public ResponseEntity<SimulationStartResponse> startWeekly(
        @RequestBody WeeklySimulationRequest request
    ) {
        String simulationId = simulationManager.startWeeklySimulation(request);
        
        return ResponseEntity.ok(new SimulationStartResponse(
            simulationId,
            "/topic/sim/" + simulationId,  // ← Topic para frontend
            request.getStartDate()
        ));
    }
    
    @PostMapping("/collapse/start")
    public ResponseEntity<SimulationStartResponse> startCollapse(
        @RequestBody CollapseSimulationRequest request
    ) {
        String simulationId = simulationManager.startCollapseSimulation(request);
        return ResponseEntity.ok(new SimulationStartResponse(simulationId, ...));
    }
    
    @PostMapping("/stop/{simulationId}")
    public ResponseEntity<Void> stopSimulation(@PathVariable String simulationId) {
        simulationManager.stopSimulation(simulationId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/pause/{simulationId}")
    public ResponseEntity<Void> pauseSimulation(@PathVariable String simulationId) {
        simulationManager.pauseSimulation(simulationId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/resume/{simulationId}")
    public ResponseEntity<Void> resumeSimulation(@PathVariable String simulationId) {
        simulationManager.resumeSimulation(simulationId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<SimulationInfo>> getActiveSimulations() {
        return ResponseEntity.ok(simulationManager.getActiveSimulations());
    }
}
```

**Con Cursor:** REST controllers son muy estándar, Cursor los completa rápido.

---

### ✅ Resumen Subfase 3.A: WebSocket/STOMP

| Tarea | Sin IA | Con Cursor | Archivos |
|-------|--------|------------|----------|
| 3.A.1 SimulationManager | 4-5h | 2-2.5h | 1 nuevo |
| 3.A.2 Scheduler Factories | 2-3h | 1-1.5h | 2 nuevos |
| 3.A.3 Actualizar Schedulers (topics) | 3-4h | 1.5-2.5h | 3 modificados |
| 3.A.4 DTOs WebSocket | 2-3h | 1-1.5h | 3-5 nuevos |
| 3.A.5 SimulationController | 3-4h | 1.5-2.5h | Modificar existente |
| **TOTAL SUBFASE 3.A** | **14-19h** | **8-11.5h** | **WebSocket** |

---

### 📦 SUBFASE 3.B: Manejo de Eventos Dinámicos (OPCIONAL)

**Nota:** Esta subfase es opcional para MVP. Permite reaccionar a eventos en tiempo real como nuevos pedidos o cancelaciones de vuelos durante una simulación.

**Objetivo:** Permitir que el sistema reaccione a eventos dinámicos en tiempo real.

### Tarea 3.1: Crear Jerarquía de `SimulationEvent`

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/events/SimulationEvent.java`

```java
package pe.edu.pucp.morapack.algos.scheduler.events;

import java.time.LocalDateTime;

public abstract class SimulationEvent {
    protected LocalDateTime eventTime;
    protected EventPriority priority;
    
    public enum EventPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public abstract void apply(SchedulerState state);
    
    public abstract boolean requiresReplanning();
    
    public LocalDateTime getEventTime() {
        return eventTime;
    }
    
    public EventPriority getPriority() {
        return priority;
    }
}
```

**Clases concretas:**

```java
public class NewOrderEvent extends SimulationEvent {
    private PlannerOrder order;
    
    public NewOrderEvent(PlannerOrder order) {
        this.order = order;
        this.eventTime = order.getOrderTime();
        this.priority = EventPriority.HIGH;
    }
    
    @Override
    public void apply(SchedulerState state) {
        state.addOrder(order);
    }
    
    @Override
    public boolean requiresReplanning() {
        return true; // Nuevo pedido requiere replanificar
    }
}

public class FlightCancellationEvent extends SimulationEvent {
    private String flightId;
    
    public FlightCancellationEvent(String flightId, LocalDateTime time) {
        this.flightId = flightId;
        this.eventTime = time;
        this.priority = EventPriority.CRITICAL;
    }
    
    @Override
    public void apply(SchedulerState state) {
        state.cancelFlight(flightId);
    }
    
    @Override
    public boolean requiresReplanning() {
        return true; // Cancelación crítica requiere replanificar INMEDIATAMENTE
    }
}

public class FlightDelayEvent extends SimulationEvent {
    private String flightId;
    private int delayMinutes;
    
    // Similar...
    
    @Override
    public boolean requiresReplanning() {
        return delayMinutes > 30; // Solo replanificar si demora > 30min
    }
}
```

**Con Cursor:**
- Jerarquía de clases es rápida con IA
- "Generate concrete event classes for: NewOrder, FlightCancellation, FlightDelay"

---

### Tarea 3.2: Implementar `EventQueue` en `SchedulerState`

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

```java
// En SchedulerState.java

private PriorityQueue<SimulationEvent> eventQueue;

public SchedulerState(...) {
    // ...
    this.eventQueue = new PriorityQueue<>(
        Comparator.comparing(SimulationEvent::getEventTime)
            .thenComparing(e -> e.getPriority().ordinal())
    );
}

public void addEvent(SimulationEvent event) {
    eventQueue.offer(event);
}

/**
 * Procesa todos los eventos que deberían ocurrir en el tiempo actual.
 * Retorna true si algún evento requiere replanificación inmediata.
 */
public boolean processEvents() {
    boolean needsReplanning = false;
    
    while (!eventQueue.isEmpty() && 
           !eventQueue.peek().getEventTime().isAfter(currentTime)) {
        SimulationEvent event = eventQueue.poll();
        event.apply(this);
        
        if (event.requiresReplanning()) {
            needsReplanning = true;
        }
    }
    
    return needsReplanning;
}
```

**Con Cursor:**
- Lógica de colas con prioridad es estándar, Cursor la completa rápidamente

---

### Tarea 3.3: Integrar Event Processing en Schedulers

**Tiempo estimado:** Sin IA: 3-4h | **Con Cursor: 1.5-2.5h** ⚡

```java
// En DailyScheduler.mainLoop()

private void mainLoop() {
    while (running) {
        try {
            // ...
            
            // 1. Procesar eventos pendientes
            boolean needsImmediateReplanning = state.processEvents();
            
            // 2. Si hay evento crítico, replanificar AHORA (fuera del ciclo normal)
            if (needsImmediateReplanning) {
                logger.warn("Critical event detected, triggering immediate replanning");
                performReplanning();
            }
            
            // 3. Replanificación normal (cada K minutos)
            if (shouldReplan()) {
                performReplanning();
            }
            
            // ...
        } catch (Exception e) {
            // ...
        }
    }
}

private void performReplanning() {
    List<PlannerOrder> ordersToProcess = state.getOrdersInPlanningHorizon();
    List<PlannerFlight> availableFlights = state.getFlightsInPlanningHorizon();
    
    if (!ordersToProcess.isEmpty()) {
        List<PlannerShipment> newShipments = tabuPlanner.plan(
            ordersToProcess,
            availableFlights,
            state.getAirportManager()
        );
        
        // Actualizar estado
        newShipments.forEach(state::addShipment);
        for (PlannerShipment shipment : newShipments) {
            state.removeOrder(shipment.getOrder());
        }
        
        webSocketService.sendUpdate(state);
    }
}
```

---

### Tarea 3.4: API REST para Eventos

**Tiempo estimado:** Sin IA: 4-5h | **Con Cursor: 2-2.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/controller/EventController.java`

```java
@RestController
@RequestMapping("/api/events")
public class EventController {
    
    private final DailyScheduler dailyScheduler;
    private final WeeklyScheduler weeklyScheduler;
    private final CollapseScheduler collapseScheduler;
    
    @PostMapping("/newOrder")
    public ResponseEntity<Void> addNewOrder(@RequestBody OrderDTO orderDTO) {
        PlannerOrder order = orderMapper.toPlannerOrder(orderDTO);
        NewOrderEvent event = new NewOrderEvent(order);
        
        // Añadir al scheduler activo
        Scheduler activeScheduler = getActiveScheduler();
        activeScheduler.getState().addEvent(event);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/cancelFlight")
    public ResponseEntity<Void> cancelFlight(@RequestBody FlightCancellationDTO dto) {
        FlightCancellationEvent event = new FlightCancellationEvent(
            dto.getFlightId(),
            LocalDateTime.now()
        );
        
        Scheduler activeScheduler = getActiveScheduler();
        activeScheduler.getState().addEvent(event);
        
        return ResponseEntity.ok().build();
    }
    
    private Scheduler getActiveScheduler() {
        // Lógica para determinar qué scheduler está activo
        // ...
    }
}
```

**Con Cursor:**
- Controllers REST son boilerplate, Cursor los genera rápidamente
- DTOs también son automáticos

---

### Tarea 3.5: Cargar Cancelaciones desde Archivos (para simulaciones)

**Tiempo estimado:** Sin IA: 3-4h | **Con Cursor: 1.5-2.5h** ⚡

**Archivo:** `pe/edu/pucp/morapack/algos/scheduler/FileEventLoader.java`

```java
@Component
public class FileEventLoader {
    
    /**
     * Carga eventos de cancelación desde archivo.
     * Formato: dd.id-vuelo
     * Ejemplo: 03.SPIM-SBBR-08:30
     */
    public List<FlightCancellationEvent> loadCancellations(String filePath, int month) {
        List<FlightCancellationEvent> events = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\.");
                int day = Integer.parseInt(parts[0]);
                String flightId = parts[1];
                
                LocalDateTime eventTime = LocalDateTime.of(2025, month, day, 0, 0);
                
                events.add(new FlightCancellationEvent(flightId, eventTime));
            }
        } catch (IOException e) {
            logger.error("Error loading cancellations from file", e);
        }
        
        return events;
    }
}
```

**Con Cursor:**
- File parsing es rápido con IA
- Manejo de errores sugerido automáticamente

---

### Tarea 3.6: Tests para Event System

**Tiempo estimado:** Sin IA: 4-5h | **Con Cursor: 1.5-2.5h** ⚡

```java
@Test
void testNewOrderEventTriggersReplanning() {
    SchedulerState state = createTestState();
    PlannerOrder order = createTestOrder();
    
    NewOrderEvent event = new NewOrderEvent(order);
    state.addEvent(event);
    
    boolean needsReplanning = state.processEvents();
    
    assertTrue(needsReplanning);
    assertTrue(state.getPendingOrders().contains(order));
}
```

---

### ✅ Resumen Fase 3: Eventos Dinámicos

| Tarea | Sin IA | Con Cursor | Archivos |
|-------|--------|------------|----------|
| 3.1 SimulationEvent | 2-3h | 1-1.5h | 4-5 nuevos |
| 3.2 EventQueue | 2-3h | 1-1.5h | SchedulerState.java |
| 3.3 Integrar en Schedulers | 3-4h | 1.5-2.5h | 3 schedulers |
| 3.4 API REST | 4-5h | 2-2.5h | Controller + DTOs |
| 3.5 FileEventLoader | 3-4h | 1.5-2.5h | 1 nuevo |
| 3.6 Tests | 4-5h | 1.5-2.5h | Test files |
| **TOTAL FASE 3** | **18-25h** | **11-15h** | **Eventos** |

---

## 🧹 FASE 4: LIMPIEZA Y OPTIMIZACIÓN

### Tarea 4.1: Eliminar `PlannerRoute` y `PlannerSegment`

**Tiempo estimado:** Sin IA: 1h | **Con Cursor: 0.5h** ⚡

```bash
# Eliminar archivos
rm pe/edu/pucp/morapack/algos/entities/PlannerRoute.java
rm pe/edu/pucp/morapack/algos/entities/PlannerSegment.java
```

**Verificar que no haya referencias:** Usar Cursor para buscar y limpiar.

---

### Tarea 4.2: Implementar Persistencia de Rutas (model.Route, model.Segment)

**Tiempo estimado:** Sin IA: 3-4h | **Con Cursor: 1.5-2h** ⚡

```java
// model/Route.java
@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    private List<Segment> segments;
    
    private LocalDateTime createdAt;
    private int totalQuantity;
    private String status; // PLANNED, IN_TRANSIT, DELIVERED
    
    // Getters, setters
}

@Entity
@Table(name = "segments")
public class Segment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route;
    
    @ManyToOne
    @JoinColumn(name = "flight_id")
    private Flight flight;
    
    private int segmentOrder; // 1, 2, 3... (orden en la ruta)
    private int quantity;
    
    // Getters, setters
}
```

**Repositories:**

```java
@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByOrderId(Long orderId);
    List<Route> findByStatus(String status);
}

@Repository
public interface SegmentRepository extends JpaRepository<Segment, Long> {
    List<Segment> findByRouteId(Long routeId);
}
```

**Mapper:**

```java
@Component
public class RouteMapper {
    
    public Route toEntity(PlannerShipment shipment, Order order) {
        Route route = new Route();
        route.setOrder(order);
        route.setTotalQuantity(shipment.getQuantity());
        route.setCreatedAt(LocalDateTime.now());
        route.setStatus("PLANNED");
        
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < shipment.getFlights().size(); i++) {
            PlannerFlight pf = shipment.getFlights().get(i);
            
            Segment segment = new Segment();
            segment.setRoute(route);
            segment.setFlight(flightRepository.findByCode(pf.getFlightCode()));
            segment.setSegmentOrder(i + 1);
            segment.setQuantity(shipment.getQuantity());
            
            segments.add(segment);
        }
        
        route.setSegments(segments);
        return route;
    }
}
```

**Con Cursor:**
- Entities JPA son casi 100% automáticas
- Repositories también
- Mappers requieren algo de lógica pero Cursor ayuda mucho

---

### Tarea 4.3: Guardar Rutas Planificadas en BD

**Tiempo estimado:** Sin IA: 2-3h | **Con Cursor: 1-1.5h** ⚡

```java
// En DailyScheduler, después de planificar:

for (PlannerShipment shipment : newShipments) {
    // Guardar en BD
    Order order = orderRepository.findById(shipment.getOrder().getId()).orElseThrow();
    Route route = routeMapper.toEntity(shipment, order);
    routeRepository.save(route);
    
    // Actualizar estado
    state.addShipment(shipment);
    state.removeOrder(shipment.getOrder());
}
```

---

### Tarea 4.4: Documentación de Arquitectura

**Tiempo estimado:** Sin IA: 1-2h | **Con Cursor: 0.5-1h** ⚡

Crear `ARCHITECTURE.md` con diagramas y explicaciones.

**Con Cursor:**
- Cursor puede generar markdown con estructura profesional
- Puede sugerir secciones importantes

---

### ✅ Resumen Fase 4: Limpieza

| Tarea | Sin IA | Con Cursor | Archivos |
|-------|--------|------------|----------|
| 4.1 Eliminar clases redundantes | 1h | 0.5h | -2 archivos |
| 4.2 Implementar Route/Segment | 3-4h | 1.5-2h | Entities + Repos |
| 4.3 Guardar en BD | 2-3h | 1-1.5h | Schedulers |
| 4.4 Documentación | 1-2h | 0.5-1h | ARCHITECTURE.md |
| **TOTAL FASE 4** | **6-9h** | **3-5h** | **Limpieza** |

---

## 📊 RESUMEN GLOBAL (ACTUALIZADO)

### Tiempos Totales

| Fase | Descripción | Sin IA | Con Cursor | Crítico |
|------|-------------|--------|------------|---------|
| **Fase 1** | Airport Capacity Manager | 20-27.5h | 13-16.75h | ⚠️ CRÍTICO |
| **Fase 2** | K + DataProviders + Schedulers | 42-56h | 26-36h | ⚠️ CRÍTICO |
| **Fase 3** | WebSocket/STOMP + Eventos | 26-42h | 15-23h | ⚠️ IMPORTANTE |
| **Fase 4** | Limpieza + Persistencia | 6-9h | 3-5h | Opcional |
| **TOTAL** | - | **94-134.5h** | **57-80.75h** | - |

### Estimación Realista con Cursor

**Tiempo total: 57-81 horas (~7-10 días laborables)**

### Desglose Detallado

**FASE 1 (Crítico):** 13-16.75h
- Airport Capacity Manager mejorado
- Integración en Tabu Search

**FASE 2 (Core):** 26-36h
- Subfase 2.A: DataProviders (13-17.5h)
  - FlightExpander, OrderDateInterpreter
  - DatabaseDataProvider, FileDataProvider
- Subfase 2.B: Schedulers (13-18.5h)
  - SchedulerState, ScenarioConfig
  - Daily/Weekly/CollapseScheduler

**FASE 3 (WebSocket):** 15-23h
- Subfase 3.A: STOMP (8-11.5h)
  - SimulationManager
  - Topics individuales
- Subfase 3.B: Eventos Dinámicos (7-11.5h) - OPCIONAL

**FASE 4 (Polish):** 3-5h
- Limpieza código
- Documentación

**En días laborables (8h/día):**
- Mínimo: 6.25 días (~1.5 semanas)
- Promedio: 7.5 días (~2 semanas)
- Máximo: 8.75 días (~2 semanas)

### Factores que Afectan el Tiempo con Cursor

#### ✅ Cursor Acelera Mucho (~50-75% reducción):
1. **Código boilerplate** (Entities, DTOs, Mappers, Repositories)
2. **Tests unitarios básicos**
3. **File parsing y utilidades**
4. **Controllers REST estándar**
5. **Getters/Setters/Constructores**
6. **Documentación JavaDoc**

#### ⚠️ Cursor Acelera Moderadamente (~30-40% reducción):
1. **Lógica de negocio compleja** (AirportCapacityManager)
2. **Integración con algoritmo existente** (Tabu Search)
3. **Threading y concurrencia** (Schedulers)
4. **Estructuras de datos custom** (SchedulerState)

#### ⏸️ Cursor Acelera Poco (~10-20% reducción):
1. **Diseño arquitectónico** (decisiones humanas necesarias)
2. **Debugging de lógica sutil**
3. **Optimización de rendimiento**
4. **Análisis de requisitos**
5. **Validación end-to-end**

---

## 📅 PLAN DE EJECUCIÓN RECOMENDADO

### Semana 1 (40 horas)

**Días 1-2: Fase 1 (Capacidades) - 14-18h**
- Día 1 mañana: PlannerAirport + AirportCapacityManager (4-5h)
- Día 1 tarde: Integrar en Greedy Allocation (2.5-3.5h)
- Día 2 mañana: Integrar en Movimientos Tabu (3.5-5h)
- Día 2 tarde: Tests + CostFunction (2-2.5h)

**Días 3-5: Fase 2 (Concepto K) - 22-32h**
- Día 3: SchedulerState + ScenarioConfig + Interfaces (4-6h)
- Día 4: DailyScheduler + WeeklyScheduler (6-8h)
- Día 5 mañana: CollapseScheduler (3-4h)
- Día 5 tarde: DatabaseDataProvider + Mappers (3.5-4h)

### Semana 2 (30 horas)

**Día 6: Fase 2 (Continuación) + Fase 3 (Inicio) - 8h**
- Mañana: Actualizar Tabu + Tests integración (2.5-4h)
- Tarde: SimulationEvent + EventQueue (2.5-3h)

**Días 7-8: Fase 3 (Eventos) - 11-15h**
- Día 7: Integrar eventos en Schedulers (1.5-2.5h)
- Día 7: API REST para eventos (2-2.5h)
- Día 8 mañana: FileEventLoader (1.5-2.5h)
- Día 8 tarde: Tests eventos (1.5-2.5h)

**Día 9: Fase 4 (Limpieza) - 3-5h**
- Eliminar clases redundantes (0.5h)
- Implementar Route/Segment (1.5-2h)
- Guardar en BD (1-1.5h)

**Día 10: Testing Final + Documentación**
- Tests end-to-end
- Documentación
- Ajustes finales

---

## 🎯 ORDEN DE PRIORIDAD

### 🔴 CRÍTICO (Debe completarse primero)
1. **Fase 1: Airport Capacity Manager** → Sistema es inválido sin esto
2. **Fase 2 (Parcial): SchedulerState + ScenarioConfig + DailyScheduler** → Core funcional

### 🟡 IMPORTANTE (Necesario para cumplir requisitos)
3. **Fase 2 (Resto): WeeklyScheduler + CollapseScheduler** → Cumplir 3 escenarios
4. **Fase 2: DataProvider + Mappers** → Conectar con BD

### 🟢 DESEABLE (Mejora UX y robustez)
5. **Fase 3: Eventos Dinámicos** → Reactividad en tiempo real
6. **Fase 4: Limpieza** → Código más limpio

### 🔵 OPCIONAL (Si hay tiempo)
7. **Optimizaciones de rendimiento**
8. **Métricas y monitoreo avanzado**
9. **UI mejorada en frontend**

---

## 🚨 RIESGOS Y MITIGACIONES

### Riesgo 1: Bugs Sutiles en AirportCapacityManager
**Probabilidad:** Media  
**Impacto:** Alto  
**Mitigación:**
- Tests exhaustivos con casos edge
- Logging detallado
- Validaciones en múltiples capas

### Riesgo 2: Integración con Tabu Search Compleja
**Probabilidad:** Media  
**Impacto:** Alto  
**Mitigación:**
- Refactorizar incrementalmente
- Mantener versión anterior funcional
- Tests de regresión

### Riesgo 3: Threading Issues en Schedulers
**Probabilidad:** Media-Alta  
**Impacto:** Medio  
**Mitigación:**
- Usar @Async de Spring correctamente
- Sincronización cuidadosa
- Monitoreo de memory leaks

### Riesgo 4: Performance en Simulación Semanal
**Probabilidad:** Media  
**Impacto:** Medio  
**Mitigación:**
- Profiling temprano
- Optimizar consultas BD
- Cachear datos estáticos

### Riesgo 5: Tiempo Subestimado
**Probabilidad:** Alta (siempre 😅)  
**Impacto:** Medio  
**Mitigación:**
- Buffer de 20% en estimaciones
- Priorizar fases críticas
- MVP funcional primero, luego polish

---

## 🎓 APRENDIZAJES Y MEJORES PRÁCTICAS

### Con Cursor/IA
1. **Usar prompts específicos:** "Generate AirportCapacityManager with these methods..." vs "make a manager"
2. **Revisar código generado:** IA puede tener bugs sutiles
3. **Iterar rápidamente:** Generar → Probar → Ajustar → Regenerar
4. **Aprovechar para boilerplate:** Entities, DTOs, Tests básicos
5. **Pensar antes de pedir:** IA acelera ejecución, no diseño

### Arquitectura
1. **Separación clara:** Planner entities vs Model entities vs DTOs
2. **Interfaces para flexibilidad:** DataProvider permite cambiar fuente de datos
3. **Estado mutable centralizado:** SchedulerState es single source of truth
4. **Event-driven para extensibilidad:** Fácil añadir nuevos tipos de eventos

### Testing
1. **Tests unitarios first:** Especialmente para lógica crítica (capacidades)
2. **Mocks para servicios externos:** BD, WebSockets
3. **Tests de integración para flows completos:** Scheduler → Tabu → BD

---

## 📚 REFERENCIAS

### Documentos Previos
1. `ANALISIS_ESTADO_ACTUAL_Y_PLAN_K.md` → Análisis inicial (41 páginas)
2. `RESPUESTA_REFACTORIZACION_MINIMA.md` → Q&A sobre PlannerRoute/Segment
3. `MANEJO_EVENTOS_DINAMICOS.md` → Diseño de eventos
4. `ANALISIS_CAPACIDADES_Y_RESTRICCIONES.md` → Problema capacidades aeropuertos

### Código de Referencia
1. `PDDS-VRP-planner-visualizer-main` → Implementación de K original
2. MoraPack `TabuSearchPlanner.java` → Algoritmo actual

### Requisitos del Curso
- 3 escenarios: Diario, Semanal, Colapso
- Simulación semanal: 30-90 minutos de ejecución
- Restricciones de capacidad: vuelos (200-400), aeropuertos (600-1000)
- Plazos: 2 días mismo continente, 3 días diferente continente

---

## ✅ CHECKLIST FINAL

### Funcionalidad
- [ ] Sistema respeta capacidades de aeropuertos (hard constraint)
- [ ] Operaciones diarias funciona en tiempo real (K=1)
- [ ] Simulación semanal completa en 30-90 min (K=120)
- [ ] Simulación colapso detecta límite del sistema (K=60)
- [ ] Eventos dinámicos procesados correctamente
- [ ] Rutas guardadas en BD
- [ ] Frontend recibe actualizaciones en tiempo real

### Calidad
- [ ] Tests unitarios para componentes críticos (>80% coverage)
- [ ] Tests de integración para flows principales
- [ ] Logging adecuado en todos los schedulers
- [ ] Manejo de errores robusto
- [ ] Sin memory leaks en simulaciones largas

### Documentación
- [ ] README actualizado con arquitectura
- [ ] JavaDoc en clases principales
- [ ] Guía de uso para cada escenario
- [ ] Diagramas de flujo

---

## 🎉 CONCLUSIÓN

Este plan completo unifica todos los análisis previos y proporciona una hoja de ruta clara para implementar el concepto K y corregir los problemas críticos en MoraPack.

**Con Cursor, el proyecto es completamente factible en 2 semanas** (50-70 horas), priorizando correctamente y aprovechando la IA para acelerar tareas repetitivas.

**Próximos pasos:**
1. Revisar y aprobar este plan
2. Configurar entorno de desarrollo
3. Comenzar con Fase 1 (Capacidades) → CRÍTICO
4. Iteraciones rápidas con tests continuos

**¡Éxito en la implementación! 🚀**
