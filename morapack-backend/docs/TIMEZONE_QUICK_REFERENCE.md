# Referencia Rápida: Timezones en MoraPack

## 🎯 Regla de Oro

**TODO el sistema usa UTC internamente. Solo se convierte a timezone local para calcular deadlines.**

---

## 📦 Pedidos (Orders)

### En la Base de Datos:
```sql
('000000001', '2025-01-02', '01:38:00', 'EBCI', 6, '0007729', 'SCHEDULED')
                                ^^^^^^^^  ^^^^
                                  UTC     Bruselas (GMT+2)
```

### Interpretación:
- **01:38:00** está en **UTC**
- Hora local en Bruselas: **03:38:00** (01:38 + 2 horas)
- Deadline (48h): **03:38:00 + 48h** en timezone de Bruselas
- Deadline en UTC: **01:38:00 + 48h** (2 días después)

---

## ✈️ Vuelos (Flights)

### En la Base de Datos:
```sql
('SKBO','SEQM', '2025-01-02', '03:34:00', '05:21:00', 300)
                                ^^^^^^^^  ^^^^^^^^
                                  UTC       UTC
```

### Interpretación:
- Ambos tiempos en **UTC**
- Bogotá (GMT-5): 22:34 → Quito (GMT-5): 00:21
- Duración: ~2 horas ✓

---

## ❌ Cancelaciones

### Formato de Archivo:
```
01.SPIM-SCEL-0800
   ^^^^-^^^^-^^^^
   Origen Dest Hora UTC
```

### Interpretación:
- **0800** = 08:00 **UTC**
- Lima (GMT-5): 03:00 hora local
- Santiago (GMT-4): 04:00 hora local

---

## ⏰ Cálculo de Deadlines

```java
// 1. orderTime en UTC
orderTime = 2025-01-02 01:38:00 (UTC)

// 2. Convertir a timezone del destino (Bruselas GMT+2)
orderTimeLocal = 2025-01-02 03:38:00 (Bruselas)

// 3. Sumar 48h en timezone del destino
deadlineLocal = 2025-01-04 03:38:00 (Bruselas)

// 4. Convertir de vuelta a UTC
deadline = 2025-01-04 01:38:00 (UTC)
```

---

## ✅ Checklist

- [ ] Datos en BD: **UTC**
- [ ] Simulación avanza: **UTC**
- [ ] Vuelos despegan/llegan: **UTC**
- [ ] Deadlines se calculan en: **Timezone del destino** → luego a UTC
- [ ] Comparaciones (arrival vs deadline): **UTC vs UTC**

---

## 🔍 Cómo Verificar

### Pedido a EBCI (Bruselas, GMT+2) a las 01:38 UTC:
```
✅ Correcto:
- orderTime: 2025-01-02T01:38:00 (UTC)
- Hora local: 03:38:00 (Bruselas)
- Deadline: 2025-01-04T01:38:00 (UTC) = 03:38:00 (Bruselas)

❌ Incorrecto:
- orderTime: 2025-01-02T01:38:00 (hora local Bruselas)
- Deadline: 2025-01-04T03:38:00 (UTC)
```

---

**Documento completo:** Ver `TIMEZONE_HANDLING.md`
