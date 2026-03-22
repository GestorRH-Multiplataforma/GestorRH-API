package com.gestorrh.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Componente de filtrado de seguridad para la interceptación de peticiones HTTP.
 * <p>
 * Este filtro se ejecuta una vez por cada solicitud (extiende de {@link OncePerRequestFilter})
 * para validar la presencia y validez de un token JWT en el encabezado {@code Authorization}.
 * Si el token es válido, establece la autenticación del usuario en el {@link SecurityContextHolder}.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class FiltroJwt extends OncePerRequestFilter {

    /**
     * Servicio para la gestión de operaciones relacionadas con JWT.
     */
    private final ServicioJwt servicioJwt;

    /**
     * Realiza el filtrado interno de la petición para gestionar la seguridad basada en JWT.
     * <p>
     * El proceso sigue estos pasos:
     * 1. Verifica si existe un encabezado 'Authorization' con el prefijo 'Bearer '.
     * 2. Extrae el token JWT del encabezado.
     * 3. Valida el token y extrae el correo del usuario y su rol.
     * 4. Si el usuario no está ya autenticado, establece el contexto de seguridad con las autoridades correspondientes.
     * 5. Continúa con la cadena de filtros.
     * </p>
     *
     * @param peticion La solicitud HTTP recibida.
     * @param respuesta La respuesta HTTP que se enviará.
     * @param cadenaFiltros La cadena de filtros de seguridad.
     * @throws ServletException Si ocurre un error en el procesamiento del servlet.
     * @throws IOException Si ocurre un error de entrada/salida.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest peticion,
            @NonNull HttpServletResponse respuesta,
            @NonNull FilterChain cadenaFiltros) throws ServletException, IOException {

        final String encabezadoAutenticacion = peticion.getHeader("Authorization");
        final String jwt;
        final String correoUsuario;

        if (encabezadoAutenticacion == null || !encabezadoAutenticacion.startsWith("Bearer ")) {
            cadenaFiltros.doFilter(peticion, respuesta);
            return;
        }

        jwt = encabezadoAutenticacion.substring(7);

        try {
            correoUsuario = servicioJwt.extraerEmail(jwt);
            String rolToken = servicioJwt.extraerRol(jwt);

            if (correoUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (servicioJwt.esTokenValido(jwt)) {
                    List<GrantedAuthority> autoridades = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + rolToken)
                    );
                    UsernamePasswordAuthenticationToken tokenAutenticacion = new UsernamePasswordAuthenticationToken(
                            correoUsuario,
                            null,
                            autoridades
                    );
                    tokenAutenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));

                    SecurityContextHolder.getContext().setAuthentication(tokenAutenticacion);
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando el JWT: " + e.getMessage());
        }

        cadenaFiltros.doFilter(peticion, respuesta);
    }
}
