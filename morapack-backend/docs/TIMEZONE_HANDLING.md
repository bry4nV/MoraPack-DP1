# Manejo de Timezones en MoraPack

## 📋 Resumen Ejecutivo

**Regla Principal:** Todo el sistema usa **UTC** como tiempo de referencia interno. Las conversiones a timezone local se realizan **únicamente** para calcular deadlines según la Indicación #16 del caso.

---

## 🌍 Contrato de Timezones

### 1. Base de Datos (moraTravelSimulation)

#### Tabla `order`:
```sql
order_date DATE        -- Fecha en UTC
order_time TIME        -- Hora en UTC
```

**Ejemplo:**
```sql
('000000001', '2025-01-02', '01:38:00', 'EBCI', 6, '0007729', 'SCHEDULED')
```

- **Hora registrada:** 01:38:00 UTC
- **Destino:** EBCI (Bruselas, GMT+2)
- **Hora local en Bruselas:** 03:38:00
- **Deadline (48h):** 03:38:00 + 48h en timezone de Bruselas = 01:38:00 UTC (+ 2 días)

#### Tabla `flight`:
```sql
flight_date      DATE  -- Fecha en UTC
departure_time   TIME  -- Hora de despegue en UTC
arrival_time     TIME  -- Hora de llegada en UTC
```

**Ejemplo:**
```sql
('SKBO', 'SEQM', '2025-01-02', '03:34:00', '05:21:00', 300)
```

- **Salida:** 03:34:00 UTC (22:34 hora local Bogotá GMT-5)
- **Llegada:** 05:21:00 UTC (00:21 hora local Quito GMT-5)
- **Duración:** ~2 horas ✓

#### Tabla `airport`:
```sql
gmt INT  -- Offset en horas respecto a UTC
```

**Ejemplos:**
- Lima (SPIM): `gmt = -5`
- Bruselas (EBCI): `gmt = 2`
- Baku (UBBB): `gmt = 4`

---

### 2. Sistema de Simulación

#### SimulationSession.java
```java
private LocalDateTime startTime;     // UTC
private LocalDateTime currentTime;   // UTC (avanza durante simulación)
```

**Importante:** La simulación avanza en tiempo UTC. Los eventos (pedidos, vuelos, cancelaciones) se comparan contra `currentTime` que está en UTC.

---

### 3. Cálculo de Deadlines (Timezone-Aware)

#### PlannerOrder.java

```java
public LocalDateTime getDeadlineInDestinationTimezone() {
    // 1. orderTime está en UTC
    // 2. Convertir a timezone del destino
    ZoneOffset destOffset = ZoneOffset.ofHours(destination.getGmt());
    ZonedDateTime orderTimeAtDest = orderTime.atZone(ZoneOffset.UTC)
                                              .withZoneSameInstant(destOffset);

    // 3. Sumar horas del deadline EN el timezone del destino
    ZonedDateTime deadlineAtDest = orderTimeAtDest.plusHours(maxDeliveryHours);

    // 4. Convertir de vuelta a UTC para comparaciones
    return deadlineAtDest.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
}
```

**Cumple con Indicación #16:**
> "El plazo de entrega se mide respecto de la hora minuto en que se hizó el envio/pedido en el uso horario del destino."

---

### 4. Cancelaciones de Vuelos

#### Formato de archivo: `dd.ORIGEN-DESTINO-HHmm`

```
01.SPIM-SCEL-0800
```

- **dd:** Día del mes (01-31)
- **HHmm:** Hora en **UTC** en formato 24h
- **Ejemplo:** `0800` = 08:00 UTC
  - En Lima (GMT-5): 03:00 hora local
  - En Bruselas (GMT+2): 10:00 hora local

**Validación:** El sistema compara `HHmm` contra `currentTime` (UTC) de la simulación.

---

## 📊 Ejemplos Prácticos

### Ejemplo 1: Pedido a Bruselas desde Lima

```sql
-- Base de Datos:
order_date = '2025-01-02'
order_time = '01:38:00'  -- UTC
destination = 'EBCI'     -- Bruselas (GMT+2)
```

**Proceso:**

1. **Registro en BD:**
   - UTC: 2025-01-02 01:38:00
   - Hora en Bruselas: 2025-01-02 03:38:00

2. **Cálculo de Deadline:**
   ```java
   // orderTime (UTC) = 2025-01-02 01:38:00
   // Convertir a Bruselas (GMT+2):
   //   → 2025-01-02 03:38:00
   // Sumar 48h en timezone de Bruselas:
   //   → 2025-01-04 03:38:00
   // Convertir de vuelta a UTC:
   //   → 2025-01-04 01:38:00
   ```

3. **Deadline Final:**
   - UTC: 2025-01-04 01:38:00
   - Hora en Bruselas: 2025-01-04 03:38:00

---

### Ejemplo 2: Vuelo Intercontinental

```sql
-- Base de Datos:
origin = 'SBBR'           -- Brasilia (GMT-3)
destination = 'EBCI'      -- Bruselas (GMT+2)
departure_time = '01:41:00'  -- UTC
arrival_time = '19:43:00'    -- UTC
```

**Interpretación:**

- **Salida (UTC):** 01:41:00 = 22:41 hora local Brasilia (día anterior)
- **Llegada (UTC):** 19:43:00 = 21:43 hora local Bruselas
- **Duración real:** ~18 horas ✓

---

### Ejemplo 3: Cancelación de Vuelo

```
Archivo: cancellations_2025_01.txt
Línea: 03.EBCI-SPIM-1800
```

**Interpretación:**

- **Día:** 3 (del mes de inicio de simulación)
- **Vuelo:** EBCI → SPIM (Bruselas → Lima)
- **Hora del vuelo:** 18:00 **UTC**
  - Hora local Bruselas: 20:00 (GMT+2)
  - Hora local Lima: 13:00 (GMT-5)

**Validación:**
```java
// currentTime (UTC) vs scheduledDepartureTime (UTC)
// Solo se cancela si el vuelo aún no ha despegado
```

---

## 🎯 Reglas de Oro

1. **Almacenamiento:** Siempre en **UTC**
2. **Simulación:** Siempre avanza en **UTC**
3. **Comparaciones:** Siempre en **UTC**
4. **Deadlines:** Calculados en **timezone del destino**, luego convertidos a UTC
5. **Métricas:** Usan deadlines timezone-aware (ya en UTC) para comparar con arrival times (UTC)

---

## ✅ Validación de Consistencia

### Para verificar que un pedido está bien:

```java
// 1. orderTime está en UTC
LocalDateTime orderTime = plannerOrder.getOrderTime();  // UTC

// 2. deadline calculado con timezone del destino
LocalDateTime deadline = plannerOrder.getDeadlineInDestinationTimezone();  // UTC

// 3. arrival time de los shipments en UTC
LocalDateTime arrival = shipment.getEstimatedArrival();  // UTC

// 4. Comparación: arrival vs deadline (ambos UTC)
boolean onTime = arrival.isBefore(deadline) || arrival.isEqual(deadline);
```

---

## 📚 Referencias

- **Indicación #16 del Caso:** "El plazo de entrega se mide respecto de la hora minuto en que se hizó el envio/pedido en el uso horario del destino."
- **DatabaseDataProvider.java:** Lectura de datos desde BD (asume UTC)
- **PlannerOrder.java:** Cálculo de deadlines timezone-aware
- **SimulationSession.java:** Avance de tiempo en UTC

---

## 🔄 Migraciones Futuras

Si en el futuro se decide cambiar el modelo a "hora local en BD":

1. Modificar `DatabaseDataProvider.convertOrderToPlanner()` para convertir de timezone destino a UTC
2. Modificar `DatabaseDataProvider.convertFlightToPlanner()` para convertir de timezone origen/destino a UTC
3. El resto del sistema (deadlines, métricas) **NO requiere cambios** porque ya opera en UTC internamente

---

**Última actualización:** 2025-11-18
**Versión:** 1.0
**Autor:** MoraPack Development Team
