# GestorRH API - AI Assistant Instructions

## 1. Stack Tecnológico Base
* **Lenguaje:** Java 21
* **Framework:** Spring Boot (v3.x / 4.x)
* **Gestor de dependencias:** Maven (`pom.xml`)
* **Base de Datos:** PostgreSQL (Producción / Docker) y H2 (Memoria para Tests y CI/CD)
* **ORM:** Spring Data JPA / Hibernate
* **Seguridad:** Spring Security + JWT (Stateless)
* **Infraestructura:** Docker, Docker Compose, GitHub Actions

---

## 2. Estructura de Paquetes

```
src/
├── main/
│   ├── java/com/gestorrh/api/
│   │   ├── annotation/        # Anotaciones Swagger propias (@ApiErroresLectura, @ApiErroresEscritura, @ApiErroresAccion)
│   │   ├── config/            # Configuración de Spring (Security, CORS, etc.)
│   │   ├── controller/        # Controladores REST — delgados, solo HTTP + seguridad + Swagger
│   │   ├── dto/               # DTOs organizados por dominio
│   │   │   ├── asignacion/
│   │   │   ├── ausencia/
│   │   │   ├── autenticacion/
│   │   │   ├── empleado/
│   │   │   ├── empresa/
│   │   │   ├── error/
│   │   │   ├── estadisticas/
│   │   │   ├── fichaje/
│   │   │   ├── reporte/
│   │   │   └── turno/
│   │   ├── entity/            # Entidades JPA: Empleado, Empresa, Fichaje, Ausencia, Turno, AsignacionTurno
│   │   │   └── enums/         # Enums del dominio
│   │   ├── exception/         # @RestControllerAdvice (GestorExcepciones) y excepciones propias
│   │   ├── repository/        # Interfaces Spring Data JPA
│   │   ├── security/          # Filtros JWT y configuración de seguridad
│   │   └── service/           # 100% de la lógica de negocio
│   ├── javadoc/               # overview.html para Javadoc
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml  # Sin valores por defecto para secretos (Fail-Fast intencionado)
└── test/
    ├── java/com/gestorrh/api/
    │   └── service/           # Tests de servicios
    └── resources/
        └── application-test.yml  # Configuración H2 para tests
```

---

## 3. Arquitectura y Patrones de Diseño

La aplicación sigue una arquitectura N-Capas estricta enfocada en Clean Code:

* **Controladores (`@RestController`):**
  * Deben ser extremadamente delgados. Solo manejan el enrutamiento HTTP, la seguridad (`@PreAuthorize`) y la documentación (`@Operation`).
  * NUNCA reciben ni devuelven Entidades JPA. Siempre usan `ResponseEntity<DTO>`.
* **Servicios (`@Service`):**
  * Contienen el 100% de la lógica de negocio.
  * Los métodos que modifican datos deben usar `@Transactional`.
  * Los métodos de solo lectura deben usar `@Transactional(readOnly = true)` para optimizar el rendimiento.
* **Repositorios (`@Repository`):**
  * Interfaces de Spring Data JPA. Uso estricto de nomenclatura en inglés para los métodos (ej. `findByEmpleadoIdEmpleadoAndFecha`).
* **Patrón DTO:**
  * Separación estricta entre petición (`PeticionXxxDTO`) y respuesta (`RespuestaXxxDTO`).
  * Construcción garantizada mediante `@Builder` de Lombok.
* **Patrón BFF (Backend For Frontend):**
  * Para el cliente Android se diseñan endpoints consolidados (ej. `GET /api/fichajes/estado-actual`) para evitar over-fetching y reducir latencia de red.

---

## 4. Reglas de Negocio Críticas

* **Fichajes y Geovallado:**
  * Empleados con turno **PRESENCIAL** requieren validación estricta de GPS (Latitud/Longitud no pueden ser `null`) contra el radio de la sede de la empresa, tanto a la entrada como a la salida.
  * Empleados en **TELETRABAJO** están exentos de geovallado (coordenadas pueden ser `null` por privacidad). No usar `@NotNull` en coordenadas GPS si el teletrabajo está permitido.
* **Auditoría:** Toda modificación manual de un fichaje por un gestor debe dejar rastro inmutable en el campo `incidencias` de la base de datos.

---

## 5. Seguridad y Control de Acceso (RBAC)

* **JWT Stateless:** Todo endpoint (salvo `/auth/login`) requiere token válido. La clave secreta (`JWT_SECRET`) debe estar en formato Base64 estricto para evitar `DecodingException`.
* **Fail-Fast en Producción:** `application-prod.yml` NO tiene valores por defecto para secretos. Si falta el `.env`, la app falla al arrancar — es un diseño intencionado, NO un bug.
* **Autorización Granular:** Los controladores usan `@PreAuthorize("hasAnyRole(...)")`. Roles: `EMPRESA`, `SUPERVISOR`, `EMPLEADO`.
* **Aislamiento de Datos (Multi-tenant lógico):** Los servicios NUNCA confían en IDs de empleado pasados por parámetro para consultas propias. Siempre se extrae la identidad del `SecurityContext` mediante `obtenerEmpleadoAutenticado()`.

---

## 6. Manejo Global de Excepciones

`@RestControllerAdvice` (`GestorExcepciones`) estandariza todas las respuestas de error bajo `RespuestaErrorDTO`.

| Situación | Excepción a lanzar | HTTP resultante |
|---|---|---|
| Error de negocio | `RuntimeException("Mensaje claro")` | 400 Bad Request |
| Recurso no encontrado | `EntityNotFoundException` | 404 Not Found |
| Concurrencia / conflicto | `ObjectOptimisticLockingFailureException` | 409 Conflict |
| Validación de DTO fallida | Anotaciones Jakarta (`@NotNull`, `@NotBlank`) → `MethodArgumentNotValidException` | 400 con lista de errores |

---

## 7. Convenciones de Código

* **Lenguaje del dominio:** Variables, clases y métodos de negocio en **español** (ej. `Fichaje`, `Empleado`, `AsignacionTurno`).
* **Lombok:** Prohibido escribir Getters, Setters o Constructores manualmente. Usar `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Slf4j`.
* **Javadoc:** Obligatorio en controladores, DTOs y servicios complejos. DocLint silenciado en `pom.xml` (`<doclint>none</doclint>`). Priorizar explicar el *porqué* sobre el *qué*. No es obligatorio documentar cada `@param` y `@return` en métodos triviales.
* **Swagger/OpenAPI:** Documentar endpoints con `@Operation`, `@ApiResponses` y las anotaciones propias `@ApiErroresLectura`, `@ApiErroresEscritura` o `@ApiErroresAccion` según corresponda.

---

## 8. Comandos Maven

```bash
# Ejecutar todos los tests (usa H2 automáticamente via application-test.yml)
./mvnw test

# Levantar la aplicación en local (requiere PostgreSQL corriendo o perfil dev)
./mvnw spring-boot:run

# Levantar con perfil dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Compilar sin ejecutar tests
./mvnw package -DskipTests

# Generar OpenAPI JSON + Javadoc (arranca la app con H2 temporalmente)
./mvnw verify -P api-docs
```

---

## 9. Pipeline CI/CD (GitHub Actions)

* **Tests (`@SpringBootTest`):** Usan `application-test.yml` con H2 en memoria.
* **Generación de Swagger:** El `pom.xml` inyecta credenciales H2 en la fase `start` de Maven para evitar `Connection Refused` al no haber PostgreSQL en el runner.
* **GitHub Pages:** El pipeline (`ci.yml`) requiere `permissions: contents: write` para publicar documentación y OpenAPI en la rama `gh-pages`.

---

## 10. Flujo de Trabajo Git

* **Ramas:** Git Flow simplificado. `main` es la rama de producción protegida — nunca hacer push directo.
  * Funcionalidades: `feature/P0-XX-descripcion`
  * Parches críticos: `hotfix/descripcion-corta`
  * Configuración/chores: `chore/descripcion`
* **Commits:** Conventional Commits obligatorio (`feat:`, `fix:`, `chore:`, `docs:`). Referenciar la issue al final (ej. `feat: añadir BFF móvil (#82)`).
* **Semantic Versioning:** La versión en `pom.xml` (`MAJOR.MINOR.PATCH`) solo se incrementa al agrupar funcionalidades en un Release Train, no por cada PR individual.

---

## 11. Al implementar una nueva feature

1. Crear `PeticionXxxDTO` con validaciones Jakarta en `dto/<dominio>/`
2. Crear `RespuestaXxxDTO` con `@Builder` en `dto/<dominio>/`
3. Añadir método al servicio con `@Transactional` y lógica de negocio completa
4. Crear o actualizar el controller con Javadoc, `@Operation` y anotación `@ApiErrores*` correcta
5. Extraer siempre la identidad del usuario desde `SecurityContext`, nunca de parámetros
6. Escribir tests en `test/java/com/gestorrh/api/service/`
7. Verificar con `./mvnw test` antes de abrir el PR
8. Rama: `feature/P0-XX-descripcion` — commit con Conventional Commits referenciando la issue
