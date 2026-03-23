package com.gestorrh.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigSwagger {

    @Bean
    public OpenAPI gestorRhOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("GestorRH API")
                        .version("1.0")
                        .description("Documentación interactiva de la API REST para el sistema GestorRH.<br><br>" +

                                "<h3>Flujo de inicio rápido (Ciclo Natural)</h3>" +
                                "<ol>" +
                                "<li><b>Registro Empresa:</b> Registra una nueva empresa en el bloque 2 o usa las credenciales de prueba.</li>" +
                                "<li><b>Login:</b> Ve al bloque 1 (Autenticación), haz login con la empresa, copia el Token JWT y pégalo en el botón <b>Authorize</b> de arriba a la derecha.</li>" +
                                "<li><b>Configuración:</b> Ya como Empresa, crea Empleados (bloque 3) y Turnos (bloque 4).</li>" +
                                "<li><b>Operación:</b> Haz login como el Empleado recién creado para obtener su Token, ponlo en <b>Authorize</b> y ya podrás Fichar (bloque 6) o pedir Ausencias (bloque 7).</li>" +
                                "</ol>" +

                                "<h3>Credenciales de prueba (Entorno Local/Dev)</h3>" +
                                "El sistema inicializa automáticamente estos datos para facilitar las pruebas:" +
                                "<ul>" +
                                "<li><b>Empresa Admin:</b> <code>admin@gestorrh.com</code> | Contraseña: <code>1234</code></li><br>" +
                                "<li><b>Empleado Test:</b> <code>empleado@gestorrh.com</code> | Contraseña: <code>1234</code></li>" +
                                "</ul><br>" +

                                "<b>Instrucciones de Seguridad:</b> Los endpoints con el candado requieren que introduzcas el Token correspondiente en el botón verde <b>Authorize</b>.")
                        .contact(new Contact().name("Equipo de Desarrollo GestorRH")))

                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aquí directamente tu token JWT (sin la palabra 'Bearer ').")));
    }
}
