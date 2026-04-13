# GestorRH 2.0 - API REST

API REST desarrollada en Spring Boot para la gestión centralizada de recursos humanos, control de turnos, ausencias y validación de fichaje móvil con geovallado.

**Portal de Documentación (API REST & Código):** [Acceder al portal interactivo](https://gestorrh-multiplataforma.github.io/GestorRH-API/) 
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

Dependiendo de si quieres desarrollar o simplemente desplegar la aplicación, los requisitos varían:

**Para Despliegue Completo (Recomendado):**
- **Docker** y **Docker Compose** instalados. (No necesitas instalar Java ni Maven, la imagen se construye y ejecuta de forma autónoma).

**Para Desarrollo Local:**
- **Java 21** instalado en el sistema.
- **Maven** para la gestión de dependencias y compilación.
- **Docker** (solo para levantar el contenedor local de PostgreSQL).

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

## Entorno de Producción (Despliegue con Docker)

El proyecto está preparado para desplegarse en cualquier servidor (agnóstico a la nube) utilizando un enfoque *Multi-stage* con Docker.

### 1. Configurar Variables de Entorno
Copia el archivo de plantilla `.env.example` en la raíz del proyecto y renómbralo a `.env`. Rellena las variables obligatorias con tus credenciales de producción (Usuario BD, Contraseña BD y Secreto JWT).

### 2. Levantar la Infraestructura Completa
La API y su base de datos aislada se construyen y orquestan automáticamente con Docker Compose. Ejecuta:
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

---

# Documentación y Pruebas (Swagger UI)

Una vez que la aplicación esté corriendo (ya sea en tu entorno local con Maven o contenedorizada con Docker), puedes probar todos los endpoints, autenticarte y ver los esquemas de datos interactivos accediendo a:

`http://localhost:8080/swagger-ui/index.html`

*(Nota: Recuerda que para probar los endpoints protegidos, primero debes usar el endpoint de Login y pegar el token JWT resultante en el botón "Authorize").*

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

- **`v0.9.0`** → infraestructura, documentación y estabilización.  
  Incluye la contenedorización agnóstica con Docker, variables de entorno, control de concurrencia optimista y datos de prueba automatizados (DataSeeder).

- **`v1.0.0`** → primera versión estable.  
  Versión de referencia para integración con clientes y futuros despliegues, con la arquitectura base consolidada, preparación para producción y licencia aplicada.

### Criterio de uso

Para integración con clientes y despliegue, la referencia será siempre la **última versión estable aprobada**, no necesariamente el último commit de la rama `main`.

---

## Licencia

Este proyecto está bajo la Licencia Apache 2.0 - mira el archivo [LICENSE](LICENSE) para más detalles.

*Eres libre de utilizar, modificar y distribuir este software, siempre y cuando se incluya una copia de la licencia y se respeten los términos de la misma.*
