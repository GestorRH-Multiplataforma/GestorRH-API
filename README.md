# GestorRH 2.0 - API REST

API REST desarrollada en Spring Boot para la gestión centralizada de recursos humanos, control de turnos, ausencias y validación de fichaje móvil con geovallado.

**Portal de Documentación (API REST & Código):** [Acceder al portal interactivo](https://frangc2510.github.io/GestorRH-API/) 
*(Desplegado automáticamente mediante pipeline CI/CD en GitHub Pages. Incluye Swagger UI y Javadoc).*

---

## Tecnologías Utilizadas
- **Java 21** & **Spring Boot 3**
- **Spring Security & JWT** (Autenticación Stateless)
- **Spring Data JPA / Hibernate**
- **PostgreSQL** (Base de datos relacional)
- **Swagger / OpenAPI** (Documentación interactiva de endpoints)
- **Maven** & **GitHub Actions** (CI/CD)
- **Docker** & **Docker Compose**

---

## Requisitos Previos
- **Java 21** instalado en el sistema.
- **Maven** para la gestión de dependencias.
- **Docker** y **Docker Compose** (para levantar la base de datos local sin instalaciones pesadas).

---

## Entorno de Desarrollo (Local)

El proyecto utiliza perfiles de Spring (`dev`, `test`, `prod`) para separar la configuración. Por defecto, arranca en el perfil `dev`.

### 1. Levantar la Base de Datos
Para no instalar PostgreSQL en tu máquina, utilizamos un contenedor Docker. Abre una terminal en la raíz del proyecto y ejecuta:
```bash
docker compose up -d
```

### 2. Arrancar la Aplicación
Una vez que el contenedor de la base de datos esté corriendo, puedes compilar y ejecutar la API con Maven:
```bash
mvn spring-boot:run
```
La API estará disponible en la ruta base: `http://localhost:8080/api`

---

## Versionado

Este proyecto utiliza **Git tags anotados** para marcar hitos funcionales y versiones relevantes de la API REST.

La estrategia de versionado sigue **Semantic Versioning** (`MAJOR.MINOR.PATCH`):

- **MAJOR**: cambios incompatibles con clientes existentes.
- **MINOR**: nuevas funcionalidades compatibles.
- **PATCH**: correcciones compatibles sin ruptura del contrato de la API.

### Hitos publicados

- **`v0.1.0`** → primera versión funcional del backend.  
  Incluye autenticación, autorización, gestión básica de empleados y una primera base de calidad.

- **`v0.8.0`** → backlog funcional principal completado.  
  Se consideran implementadas las funcionalidades principales de negocio de la API: seguridad, empresa, empleados, turnos, asignaciones, ausencias y fichajes.

### Próximos hitos previstos

- **`v0.9.0`** → versión casi lista.  
  Incluirá la documentación y estabilización técnica previas a la primera release estable.

- **`v1.0.0`** → primera versión estable.  
  Será la versión de referencia para integración con clientes y futuros despliegues, una vez completada la refactorización importante y la preparación de despliegue.

### Criterio de uso

Para integración con clientes y despliegue, la referencia será siempre la **última versión estable aprobada**, no necesariamente el último commit de la rama `main`.
