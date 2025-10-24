# Developer run instructions — Morapack backend

Este documento recoge los pasos mínimos para que un desarrollador pueda levantar el backend localmente, probar endpoints y crear nuevos endpoints siguiendo las convenciones del repositorio.

---

## 1) Prerrequisitos

- Java 21 JDK instalado y disponible en PATH.
- Git.
- MySQL o acceso a la base de datos `moraTravel` (o copia del `mysqldump`).
- En Windows PowerShell usar el wrapper `./mvnw` (desde la carpeta del backend).

---

## 2) Clonar el repositorio

```powershell
git clone https://github.com/<org>/<repo>.git
cd MoraPack-DP1/morapack-backend
git checkout <branch-name>
```

---

## 3) Configurar la conexión a la base de datos (profile `local`)

El proyecto usa perfiles Spring. Edita `src/main/resources/application-local.properties` con tus credenciales.

Plantilla mínima:

```
spring.profiles.active=local

spring.datasource.url=jdbc:mysql://<DB_HOST>:3306/moraTravel
spring.datasource.username=<DB_USER>
spring.datasource.password=<DB_PASS>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.globally_quoted_identifiers=false
```

Notas importantes:
- Comprueba el nombre real de la tabla de pedidos en la base de datos: puede ser `Pedido`, `pedido` o `orders`. La entidad JPA `pe.edu.pucp.morapack.model.Order` actualmente usa `@Table(name = "orders")`. Si la BD tiene otro nombre, o cambias la convención, o bien renombra la tabla en la BD o cambia la anotación `@Table` en la entidad.
- Haz un backup antes de tocar la BD (ver sección Backup).

---

## 4) Backup (recomendado antes de cualquier cambio en la BD)

```powershell
mysqldump -h <host> -u <user> -p moraTravel > moraTravel_backup.sql
```

---

## 5) Compilar y ejecutar (PowerShell)

- Compilar:
```powershell
./mvnw -DskipTests clean package
```

- Ejecutar con el perfil `local`:
```powershell
java -jar target/Morapack-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

- Alternativa en desarrollo (hot run):
```powershell
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 6) Probar la API básica

- Probar el endpoint de orders:
```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/orders -Method Get
```

- Swagger UI (interactuar con la API):
  - http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON:
  - http://localhost:8080/v3/api-docs

Si el endpoint devuelve error 500 con mensaje `Table 'moraTravel.orders' doesn't exist`, revisa el nombre/estado de la tabla en la BD (ver sección DB mismatch).

---

## 7) Crear un endpoint nuevo (convenciones y ejemplo)

Paquetes por convención:
- Controller: `pe.edu.pucp.morapack.controller`
- Service: `pe.edu.pucp.morapack.service`
- Repository: `pe.edu.pucp.morapack.repository`
- DTOs: `pe.edu.pucp.morapack.dto`
- Entidades JPA: `pe.edu.pucp.morapack.model`

Ejemplo mínimo (GET /api/shipments/{id}):

Controller:
```java
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentService svc;
    public ShipmentController(ShipmentService svc) { this.svc = svc; }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentDto> get(@PathVariable String id) {
        return svc.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
```

Service (skeleton):
```java
@Service
public class ShipmentService {
    private final ShipmentRepository repo;
    public Optional<ShipmentDto> findById(String id) {
        return repo.findById(id).map(this::toDto);
    }
}
```

Repository (Spring Data JPA):
```java
public interface ShipmentRepository extends JpaRepository<Shipment, String> { }
```

Buenas prácticas:
- Validar inputs con `@Valid` y anotaciones (`@NotNull`, `@Size`, etc).
- Mantener la lógica transaccional en `@Service` y usar `@Transactional` cuando corresponda.
- Reutilizar DTOs existentes en `pe.edu.pucp.morapack.dto`.

---

## 8) Documentar el endpoint (Swagger / OpenAPI)

El proyecto ya incluye springdoc. Para documentar operaciones puedes usar `@Operation`, `@Parameter` y `@Schema` (io.swagger.v3.oas.annotations).

Ejemplo:
```java
@Operation(summary = "Get shipment by id", description = "Devuelve un shipment por su id")
@GetMapping("/{id}")
public ResponseEntity<ShipmentDto> get(@PathVariable String id) { ... }
```

Exportar spec OpenAPI a fichero (desde la máquina donde se ejecuta la app):
```powershell
Invoke-RestMethod -Uri http://localhost:8080/v3/api-docs -Method Get | Out-File openapi.json
```

---

## 9) Git: commit y PR (plantilla rápida)

- Crear branch y commitear:
```powershell
git checkout -b feat/<ticket>-add-shipment-endpoint
git add .
git commit -m "feat: add shipment endpoint"
git push origin feat/<ticket>-add-shipment-endpoint
```

- PR description: objetivo del cambio, endpoints añadidos, contract/inputs/outputs, cómo probar (curl/PowerShell), y cualquier cambio en BD necesario.

Checklist sugerido para PR:
- [ ] Build pasa localmente: `./mvnw -DskipTests clean package`
- [ ] App arranca con profile `local`
- [ ] Endpoint probado con `curl` / `Invoke-RestMethod`
- [ ] Swagger actualizado
- [ ] DB impacts documentados (rename/migration)

---


---

## 10) WebSocket y Simulación Tabu Search en Tiempo Real

### 10.1) Configuración WebSocket

El backend ahora incluye soporte para WebSocket/STOMP para transmitir resultados del algoritmo Tabu Search en tiempo real.

**Configuración:** `pe.edu.pucp.morapack.config.WebSocketConfig`
- Endpoint: `/ws` (SockJS)
- Broker: `/topic`
- Destination prefix: `/app`

### 10.2) Endpoints de Simulación

**Iniciar simulación:**
```
STOMP SEND → /app/tabu/init
Body: {"seed": 1234567890, "snapshotMs": 500}
```

**Detener simulación:**
```
STOMP SEND → /app/tabu/stop
Body: {}
```

**Recibir actualizaciones:**
```
STOMP SUBSCRIBE → /topic/tabu-simulation
Recibe: {
  "meta": {"iteration": 42, "bestCost": 324260.00, "running": true, "snapshotId": 5},
  "aeropuertos": [...],
  "itinerarios": [...]
}
```

### 10.3) Probar WebSocket con Demo HTML

**Página de prueba incluida:** `http://localhost:8080/demo-tabu.html`

Pasos:
1. Iniciar el backend: `./mvnw spring-boot:run`
2. Abrir navegador: `http://localhost:8080/demo-tabu.html`
3. Click **"🔌 Connect"** → Debería mostrar "✅ STOMP (SockJS) connected!"
4. Click **"▶️ Start Simulation"** → Verás snapshots en tiempo real con ~94 itinerarios
5. Click **"⏹️ Stop"** para detener

**Lo que muestra:**
- Número de itinerarios generados
- Iteración actual del algoritmo
- Mejor costo encontrado
- Estado de ejecución (running/stopped)
- Detalles de cada itinerario (segmentos, posiciones, vuelos)

### 10.4) Conectar Frontend React

El frontend React (`morapack-frontend`) se conecta automáticamente al WebSocket.

**Prerrequisitos:**
- Node.js 18+ (recomendado: Node 20 LTS)
- Backend corriendo en `http://localhost:8080`

**Iniciar frontend:**
```powershell
cd morapack-frontend
npm install  # Solo la primera vez
npm run dev
```

**Acceder:** `http://localhost:3000/simulacion`

**Funcionalidad:**
- 🟢 Indicador de conexión WebSocket en tiempo real
- ▶️ Botón "Iniciar Simulación" → Genera nueva solución con Tabu Search
- ⏹️ Botón "Detener" → Para la simulación actual
- 🗺️ Mapa interactivo mostrando ~94 aviones volando en tiempo real
- 🎮 Controles de velocidad y pausa de animación

### 10.5) Datos de Entrada

**CSV requeridos** (en `morapack-backend/data/`):
- `airports_real.txt` - Aeropuertos con coordenadas GPS
- `flights.csv` - Vuelos disponibles (origen, destino, horarios, capacidad)
- `pedidos.csv` - Pedidos a procesar (destino, cantidad, fecha)

**Nota:** El sistema genera automáticamente vuelos para 2 días (hoy y mañana) para dar flexibilidad al algoritmo.

### 10.6) Algoritmo Tabu Search

**Implementación:** `pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlanner`

**Características:**
- Generación de solución inicial con greedy dinámico
- Optimización mediante Tabu Search (movimientos: Split, Merge, Transfer, Reroute)
- Snapshots periódicos enviados por WebSocket
- Detección de mejoras y métricas en tiempo real
- ~94 shipments típicamente generados para 250 pedidos

**Resultados mostrados:**
- Número de shipments (itinerarios)
- Costo total de la solución
- Rutas directas vs conexiones
- Tiempo de entrega promedio
- Productos asignados

---

## 11) Troubleshooting rápido

**Backend:**
- Puerto 8080 ocupado: cambiar `server.port` en application properties o matar proceso que usa el puerto.
- Tipos Java/DB inconsistentes: si cambias `VARCHAR` a `Long` o viceversa, actualiza DTOs y entidades.
- Asegúrate de ejecutar `./mvnw -DskipTests clean package` después de cambios en código y detener el proceso anterior antes de ejecutar el nuevo jar.

**WebSocket:**
- **"No itinerarios"**: Verifica que los archivos CSV (`flights.csv`, `pedidos.csv`, `airports_real.txt`) existen en `morapack-backend/data/`
- **Pedidos en el pasado**: El sistema ajusta automáticamente fechas antiguas a hoy/mañana
- **ERR_CONNECTION_REFUSED en frontend**: Backend no está corriendo en puerto 8080
- **WebSocket desconectado**: Verifica CORS (permitido en `WebSocketConfig`) o firewall local

**Frontend:**
- **Node.js < 18**: Next.js 15 requiere Node 18+. Instala Node 20 LTS desde https://nodejs.org/
- **Puerto 3000 ocupado**: Cambiar en `package.json` o matar proceso con `netstat -ano | findstr :3000`
- **"Cannot find module"**: Ejecutar `npm install` en `morapack-frontend/`

---