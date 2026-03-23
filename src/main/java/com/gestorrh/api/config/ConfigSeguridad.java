package com.gestorrh.api.config;

import com.gestorrh.api.security.FiltroJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Clase de configuración global de seguridad de Spring Security.
 * <p>
 * Define las políticas de acceso a los diferentes endpoints de la API, la gestión de sesiones (sin estado),
 * el codificador de contraseñas y la integración del filtro personalizado para tokens JWT.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ConfigSeguridad {

    /**
     * Filtro personalizado para la validación de tokens JWT.
     */
    private final FiltroJwt filtroJwt;

    /**
     * Define el bean para el codificador de contraseñas de la aplicación.
     * <p>
     * Utiliza el algoritmo {@link BCryptPasswordEncoder} para realizar el hashing seguro de las contraseñas.
     * </p>
     *
     * @return Una instancia de {@link PasswordEncoder}.
     */
    @Bean
    public PasswordEncoder passwordCodificador() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura la cadena de filtros de seguridad (Security Filter Chain).
     * <p>
     * Define las siguientes reglas:
     * </p>
     * <ol>
     *   <li>Deshabilitar CSRF al ser una API stateless.</li>
     *   <li>Política de creación de sesiones STATELESS (sin sesión en servidor).</li>
     *   <li>Permisos de acceso: rutas públicas para autenticación, registro y errores, el resto requiere autenticación.</li>
     *   <li>Añadir el filtro {@link FiltroJwt} antes del filtro estándar de autenticación de Spring.</li>
     * </ol>
     *
     * @param http El objeto {@link HttpSecurity} para configurar la seguridad web.
     * @return La cadena de filtros configurada.
     * @throws Exception Si ocurre algún error durante la configuración.
     */
    @Bean
    public SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/error", "/api/empresas/registro").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
