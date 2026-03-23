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

### 3. Probar la API (Swagger)
Una vez arrancada la aplicación, puedes ver todos los endpoints, sus parámetros y hacer pruebas reales desde el navegador accediendo a la interfaz de Swagger (OpenAPI):
**[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**