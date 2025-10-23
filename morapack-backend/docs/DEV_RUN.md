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

## 10) Troubleshooting rápido

- Puerto 8080 ocupado: cambiar `server.port` en application properties o matar proceso que usa el puerto.
- Tipos Java/DB inconsistentes: si cambias `VARCHAR` a `Long` o viceversa, actualiza DTOs y entidades.
- Asegúrate de ejecutar `./mvnw -DskipTests clean package` después de cambios en código y detener el proceso anterior antes de ejecutar el nuevo jar.

---