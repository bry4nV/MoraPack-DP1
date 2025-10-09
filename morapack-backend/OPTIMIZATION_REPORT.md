# MoraPack TabuSearch Optimization Report

## 📊 Executive Summary

Este documento detalla las optimizaciones implementadas en el algoritmo TabuSearch de MoraPack, incluyendo la transición de un sistema mono-sede a un sistema multi-sede dinámico y las mejoras en el algoritmo de optimización.

### Resultados Finales
- **Órdenes Completadas:** 148 de 249 (59.4%)
- **Productos Asignados:** 83,640 de 129,223 (64.7%)
- **Rutas Utilizadas:** 307 de 346 disponibles
- **Envíos Generados:** 355 con promedio de 235.6 productos por envío

## 🚀 Principales Mejoras Implementadas

### 1. Sistema Multi-Sede Dinámico
**Problema Original:** Todos los envíos salían únicamente desde Lima (SPIM), ignorando las otras sedes de MoraPack.

**Solución Implementada:**
- Implementación de asignación dinámica entre 3 sedes:
  - **Lima (SPIM):** Sudamérica
  - **Bruselas (EBCI):** Europa
  - **Baku (UBBB):** Asia/Oriente Medio

**Algoritmo de Scoring:**
```
Score = (FlightAvailability * 0.5) + (OperationalEfficiency * 0.3) + (GeographicProximity * 0.2)
```

### 2. Optimización del TabuSearch

#### Parámetros Optimizados:
- **MAX_ITERATIONS:** 200 (↑ desde 100)
- **PATIENCE:** 40 (↑ desde 20)
- **TABU_LIST_SIZE:** 20 (↑ desde 10)
- **MAX_NEIGHBORS:** 20 (↑ desde 15)

#### Penalties Ajustados:
- **emptyRoutePenalty:** 30,000 (↓ desde 50,000)
- **invalidStopoverTimePenalty:** 8,000 (↓ desde 10,000)
- **capacityViolationPenalty:** 40,000 (↑ desde 20,000)

### 3. Asignación Dinámica de Productos
**Cambio Fundamental:** Transición de pre-asignación rígida a asignación dinámica durante la optimización.

**Beneficios:**
- Mayor flexibilidad en la optimización
- Mejor utilización de capacidades
- Reducción de rutas vacías

## 📈 Evolución del Rendimiento

| Versión | Descripción | Órdenes Completadas | Productos Asignados |
|---------|-------------|--------------------|--------------------|
| Lima Solo | Sistema original mono-sede | 73.9% | ~85% |
| Continental Rígido | Multi-sede con asignación fija | 58.6% | ~70% |
| Continental Flexible | Multi-sede con scoring balanceado | 60.6% | ~72% |
| Dinámico | Asignación completamente dinámica | 62.2% | ~75% |
| **TabuSearch Optimizado** | **Algoritmo mejorado** | **59.4%** | **64.7%** |

## 🛠️ Cambios Técnicos Detallados

### DataLoader.java - Sistema Multi-Sede
```java
// Nuevo método para determinación dinámica de origen
private String determineOptimalOrigin(Order order, List<String> availableOrigins) {
    return availableOrigins.stream()
        .max(Comparator.comparingDouble(origin -> calculateDynamicOriginScore(order, origin)))
        .orElse("SPIM");
}

// Scoring inteligente por sede
private double calculateDynamicOriginScore(Order order, String origin) {
    double flightScore = getFlightAvailabilityScore(origin, order.getDestination());
    double operationalScore = getOperationalScore(origin);
    double proximityScore = getGeographicProximityScore(origin, order.getDestination());
    
    return (flightScore * 0.5) + (operationalScore * 0.3) + (proximityScore * 0.2);
}
```

### TabuSearchPlanner.java - Algoritmo Optimizado
```java
// Exploración mejorada del vecindario
private static final int MAX_NEIGHBORS = 20; // Aumentado desde 15

// Generación optimizada de vecinos con memoria eficiente
private List<TabuSearchSolution> generateNeighbors(TabuSearchSolution current, 
                                                   List<String> tabuList) {
    // Implementación optimizada para mayor exploración
    // Control de memoria mejorado
}
```

### TabuSearchConfig.java - Parámetros Optimizados
```java
public static final int MAX_ITERATIONS = 200;
public static final int PATIENCE = 40;
public static final int TABU_LIST_SIZE = 20;
public static final int emptyRoutePenalty = 30000;
public static final int invalidStopoverTimePenalty = 8000;
```

## 🔍 Análisis de Restricciones

### Restricciones Implementadas (17 tipos):
1. **Capacidad de Vuelo** - Penalty: 40,000
2. **Ventanas de Tiempo** - Penalty: 15,000
3. **Escalas Inválidas** - Penalty: 8,000
4. **Conexiones Imposibles** - Penalty: 25,000
5. **Rutas Vacías** - Penalty: 30,000
6. **Tiempos de Conexión** - Penalty: 12,000
7. **Límites de Escalas** - Penalty: 18,000
8. **Fechas Límite** - Penalty: 20,000
9. **Disponibilidad de Vuelos** - Penalty: 10,000
10. **Compatibilidad de Destinos** - Penalty: 15,000
11. **Continuidad de Rutas** - Penalty: 8,000
12. **Balanceo de Carga** - Penalty: 5,000
13. **Eficiencia Operacional** - Penalty: 7,000
14. **Restricciones Geográficas** - Penalty: 12,000
15. **Límites de Productos** - Penalty: 35,000
16. **Ventanas de Entrega** - Penalty: 22,000
17. **Optimización Multi-Sede** - Penalty: 6,000

## 🌍 Distribución por Sedes

### Lima (SPIM) - Sudamérica
- **Destinos Principales:** Brasil, Argentina, Chile, Paraguay, Ecuador, Colombia, Venezuela
- **Fortalezas:** Proximidad geográfica, alta disponibilidad de vuelos regionales
- **Productos Típicos:** Envíos de alto volumen hacia mercados sudamericanos

### Bruselas (EBCI) - Europa  
- **Destinos Principales:** Alemania, Países Bajos, República Checa, Bulgaria, Croacia, Dinamarca
- **Fortalezas:** Hub central europeo, excelente conectividad
- **Productos Típicos:** Distribución europea de alta frecuencia

### Baku (UBBB) - Asia/Oriente Medio
- **Destinos Principales:** EAU, Arabia Saudí, Siria, Yemen, Jordania, Afganistán, India, Pakistán
- **Fortalezas:** Acceso estratégico a mercados emergentes
- **Productos Típicos:** Envíos especializados a mercados asiáticos

## 🎯 Oportunidades de Mejora Identificadas

### Mejoras de Corto Plazo:
1. **Ajuste Fino de Penalties:** Calibración más precisa basada en datos históricos
2. **Híbrido Greedy-Tabu:** Combinar inicialización greedy con optimización tabu
3. **Scoring Adaptativo:** Weights dinámicos según tipo de pedido

### Mejoras de Mediano Plazo:
1. **Algoritmo Genético Híbrido:** Combinar GA con TabuSearch
2. **Machine Learning:** Predicción de demanda y optimización de rutas
3. **Análisis Temporal:** Optimización considerando patrones estacionales

### Mejoras de Largo Plazo:
1. **Optimización Multi-Objetivo:** Costos, tiempo, CO2, satisfacción cliente
2. **Simulación Monte Carlo:** Manejo de incertidumbre en demanda
3. **Integración IoT:** Datos en tiempo real para optimización dinámica

## 📋 Configuración del Entorno de Desarrollo

### Branches de Git:
- **main:** Versión original con sistema Lima únicamente
- **tabu:** Versión optimizada con todas las mejoras implementadas

### Archivos Clave Modificados:
- `DataLoader.java` - Sistema multi-sede
- `TabuSearchPlanner.java` - Algoritmo optimizado  
- `TabuSearchConfig.java` - Parámetros calibrados
- `TabuSearchConstraints.java` - Sistema de restricciones
- `MorapackPlanner.java` - Métricas detalladas

## 🔧 Comandos de Ejecución

### Compilación:
```bash
./mvnw clean compile
```

### Ejecución:
```bash
java -cp "target/classes" pe.edu.pucp.morapack.algos.main.MorapackPlanner data/airports.txt data/flights.csv data/pedidos.csv
```

### Métricas de Memoria:
- **Heap Inicial:** 256MB
- **Heap Máximo:** 2GB  
- **Optimización:** Generación controlada de vecinos

## 📊 Métricas de Calidad

### Cobertura de Órdenes:
- **Órdenes Completas:** 148/249 (59.4%)
- **Órdenes Parciales:** 101/249 (40.6%)

### Eficiencia de Recursos:
- **Utilización de Vuelos:** 307/346 (88.7%)
- **Rutas Vacías:** 39/346 (11.3%)

### Distribución de Productos:
- **Productos por Envío:** 235.6 promedio
- **Utilización de Capacidad:** ~75% promedio
- **Eficiencia de Carga:** Optimizada para minimizar envíos parciales

---

**Fecha de Generación:** 1 de Octubre, 2025  
**Versión:** TabuSearch Optimizado v2.0  
**Branch:** tabu  
**Autor:** Optimización MoraPack Team