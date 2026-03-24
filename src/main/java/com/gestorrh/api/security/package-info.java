/**
 * Componentes encargados de la autenticación y autorización del sistema.
 * <p>
 * Implementa la seguridad sin estado (stateless) basada en tokens JWT. Incluye el
 * servicio de generación y validación de firmas (ServicioJwt) y el filtro de red
 * (FiltroJwt) que protege los endpoints privados.
 * </p>
 */
package com.gestorrh.api.security;