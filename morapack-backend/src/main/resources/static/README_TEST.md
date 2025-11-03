# 🧪 MoraPack Simulation Test Interface

## 📋 Descripción

Esta es una interfaz web simple para probar el sistema de simulación con WebSocket/STOMP en tiempo real.

## 🚀 Cómo usar

### 1. **Iniciar el backend**

```bash
cd morapack-backend
./mvnw.cmd spring-boot:run
```

O desde tu IDE (Run `MorapackApplication.java`)

### 2. **Abrir el navegador**

Una vez que el backend esté corriendo, abre:

```
http://localhost:8080/simulation-test.html
```

### 3. **Probar los controles**

#### **A. Configuración:**
- **Scenario Type:** Elige entre WEEKLY, COLLAPSE o DAILY
- **Custom K:** (Opcional) Personaliza el valor de K
  - K=14 → 144 iteraciones para 7 días (default WEEKLY)
  - K=24 → 84 iteraciones (más rápido)
  - K=75 → Simulación hasta colapso (COLLAPSE)

#### **B. Controles:**
- **▶️ Start:** Inicia la simulación con la configuración seleccionada
- **⏸️ Pause:** Pausa la simulación (mantiene el estado)
- **▶️ Resume:** Continúa desde donde se pausó
- **⏹️ Stop:** Detiene completamente la simulación
- **↺ Reset:** Reinicia todo a estado inicial

#### **C. Velocidad:**
- **0.5x:** Mitad de velocidad (ver detalles)
- **1x:** Velocidad normal (default)
- **2x:** Doble velocidad
- **5x:** 5 veces más rápido
- **10x:** 10 veces más rápido

### 4. **Qué observar**

#### **Status Section:**
- **Estado actual:** IDLE, STARTING, RUNNING, PAUSED, STOPPED, COMPLETED, ERROR
- **Barra de progreso:** % completado de la simulación
- **Iteration:** Iteración actual / total esperado
- **Speed:** Velocidad actual de ejecución
- **Simulated Time:** Tiempo simulado (no tiempo real)

#### **Latest Results:**
- Muestra resumen de la última iteración
- Número de aeropuertos
- Número de itinerarios
- Detalles de rutas

#### **Activity Log:**
- Registro cronológico de eventos
- Mensajes del WebSocket
- Estados de conexión

## 🔍 Qué valida este test

✅ **Conexión WebSocket/STOMP:**
- Verifica que el frontend puede conectarse al backend
- Muestra estado de conexión en tiempo real

✅ **Control de simulación:**
- Start, Pause, Resume, Stop, Reset funcionan correctamente
- Cambio de velocidad en tiempo real

✅ **Actualizaciones en tiempo real:**
- Progreso de simulación actualizado automáticamente
- Resultados de cada iteración

✅ **Múltiples usuarios:**
- Puedes abrir múltiples pestañas del navegador
- Cada pestaña tiene su propia simulación independiente

## 🐛 Troubleshooting

### **No se conecta al WebSocket:**
1. Verifica que el backend esté corriendo en `http://localhost:8080`
2. Revisa la consola del navegador (F12) para errores
3. Verifica que no haya firewall bloqueando el puerto 8080

### **La simulación no inicia:**
1. Verifica que los archivos CSV existan en `data/`:
   - `airports.txt`
   - `flights.csv`
   - `pedidos_generados.csv`
2. Revisa los logs del backend para errores

### **Botones deshabilitados:**
- Los botones se habilitan/deshabilitan según el estado
- Por ejemplo: "Resume" solo se activa cuando está PAUSED

## 📊 Interpretación de resultados

### **WEEKLY (K=14):**
- **Duración:** Simula 7 días
- **Iteraciones:** ~144 (depende de Sc=70 min)
- **Uso:** Simulación semanal completa

### **COLLAPSE (K=75):**
- **Duración:** Hasta que el sistema colapse
- **Iteraciones:** Depende de cuándo se alcance capacidad máxima
- **Uso:** Prueba de estrés del sistema

### **DAILY (K=1):**
- **Duración:** Operaciones en tiempo real
- **Iteraciones:** Continuas
- **Uso:** Operaciones día a día (no recomendado para pruebas rápidas)

## 🎯 Próximos pasos

Una vez validado que funciona:

1. **Integrar con tu frontend real** (React/Next.js)
2. **Usar los mismos endpoints:**
   - Conectar a: `ws://localhost:8080/ws`
   - Enviar mensajes a: `/app/simulation/control`
   - Recibir en: `/user/queue/simulation`
3. **Reutilizar la lógica de manejo de estados**

## 📚 Referencia de mensajes

### **Mensaje de control (Frontend → Backend):**
```json
{
  "action": "START",
  "scenarioType": "WEEKLY",
  "customK": 14,
  "speedMultiplier": 1.0
}
```

**Acciones válidas:**
- `START` - Iniciar simulación
- `PAUSE` - Pausar
- `RESUME` - Reanudar
- `STOP` - Detener
- `RESET` - Reiniciar
- `SPEED` - Cambiar velocidad

### **Actualización de estado (Backend → Frontend):**
```json
{
  "state": "RUNNING",
  "message": "Simulation in progress",
  "timestamp": "2025-10-30T15:30:00",
  "currentIteration": 42,
  "totalIterations": 144,
  "progressPercentage": 29.17,
  "simulatedTime": "2025-12-03T14:30:00",
  "currentSpeed": 1.0,
  "latestResult": {
    "aeropuertos": [...],
    "itinerarios": [...]
  }
}
```

## ⚠️ Notas importantes

1. **Cada pestaña = Usuario diferente:** Si abres múltiples pestañas, cada una tendrá su sesión independiente
2. **No persiste en BD:** Las simulaciones WEEKLY/COLLAPSE NO se guardan en base de datos (solo DAILY lo hará)
3. **Solo para testing:** Esta interfaz es para probar el backend, no para producción

---

**¿Preguntas?** Revisa los logs del navegador (F12 → Console) y del backend para más detalles.



