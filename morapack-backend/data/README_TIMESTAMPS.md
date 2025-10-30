# Timestamp Format Documentation

## ⏰ **All Timestamps are in UTC**

This project uses **UTC (Coordinated Universal Time)** for all date and time values.

### Why UTC?

1. **Consistency**: All calculations are done in the same timezone
2. **Simplicity**: No timezone conversion bugs
3. **Industry Standard**: Airlines and logistics companies use UTC internally
4. **No DST Issues**: No daylight saving time complications

---

## 📅 **File Formats**

### **orders.csv**
```csv
timestamp,destination,quantity,clientId
2025-12-04T16:22:00,EDDI,344,6084676
```

- **Format**: ISO 8601 (`YYYY-MM-DDTHH:MM:SS`)
- **Timezone**: UTC (no Z suffix, but assumed UTC)
- **Date Range**: December 1-31, 2025

### **flights.csv** (when using templates)
```csv
origin,destination,departureTime,arrivalTime,capacity,cost
SPIM,SCEL,03:11,10:15,340,1500.0
```

- **Format**: Time only (`HH:MM`)
- **Timezone**: UTC
- **Note**: These are recurring daily flights

---

## 🌍 **Airport GMT Offsets**

The `PlannerAirport` class has a `gmt` field that stores the timezone offset:

```java
Lima (SPIM):      GMT -5
Santiago (SCEL):  GMT -3
Amsterdam (EHAM): GMT +1
Dubai (OMDB):     GMT +4
```

**Important**: This field is **ONLY used when expanding flight templates**:
- Flight CSVs have times in **local time** of each airport
- `FlightExpander` converts local times to UTC using GMT offsets
- After conversion, all calculations are done in UTC

---

## 🔄 **Frontend Display**

If you need to display times in local timezone:

```typescript
// Backend sends: "2025-12-04T16:22:00"
const utcDate = new Date("2025-12-04T16:22:00Z");

// JavaScript automatically converts to user's local timezone
console.log(utcDate.toLocaleString()); 
// Output: "12/4/2025, 11:22:00 AM" (if user is in Lima, GMT-5)
```

---

## 📝 **Example Calculation**

### **Flight Time Conversion:**

**Flight CSV**: `SPIM,OMDB,02:04,07:39` (Lima → Dubai)

```java
// Departure: 02:04 Lima local (GMT-5)
02:04 - (-5) = 02:04 + 5 = 07:04 UTC

// Arrival: 07:39 Dubai local (GMT+4)  
07:39 - (+4) = 07:39 - 4 = 03:39 UTC (next day)

// Flight duration: ~20 hours (realistic for transatlantic)
```

### **Order Processing:**

**Order placed**: `2025-12-01T08:00:00` (UTC)  
**Flight departs**: `2025-12-01T07:04:00` (UTC) - converted from Lima local  
**Flight arrives**: `2025-12-02T03:39:00` (UTC) - converted from Dubai local  
**Delivery time**: (2025-12-02T03:39) - (2025-12-01T08:00) = **19.65 hours**

All comparisons in UTC! ✅

---

## ⚠️ **Important Note**

If you need to add proper timezone support in the future:

1. Use `ZonedDateTime` instead of `LocalDateTime`
2. Convert all timestamps to UTC before calculations
3. Consider Daylight Saving Time (DST)
4. Update the cost function and validation logic

**Estimated effort**: 3-4 hours

