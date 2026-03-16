package com.gestorrh.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeofencingServiceTest {

    private final GeofencingService geofencingService = new GeofencingService();

    @Test
    @DisplayName("Fichaje válido: El empleado está exactamente en la sede (0 metros)")
    void esFichajeValido_MismasCoordenadas() {
        Double latSede = 40.4168;
        Double lonSede = -3.7038;
        Integer radio = 50;

        boolean resultado = geofencingService.esFichajeValido(latSede, lonSede, latSede, lonSede, radio);
        assertTrue(resultado, "Debería ser válido estar en las mismas coordenadas");
    }

    @Test
    @DisplayName("Fichaje inválido: El empleado está demasiado lejos (fuera del radio)")
    void esFichajeValido_FueraDeRadio() {
        Double latSede = 40.4168;
        Double lonSede = -3.7038;

        Double latEmpleado = 40.4530;
        Double lonEmpleado = -3.6883;

        Integer radio = 100;

        boolean resultado = geofencingService.esFichajeValido(latEmpleado, lonEmpleado, latSede, lonSede, radio);
        assertFalse(resultado, "Debería ser inválido por estar a varios kilómetros");
    }

    @Test
    @DisplayName("Fichaje inválido: Faltan datos de GPS")
    void esFichajeValido_DatosNulos() {
        boolean resultado = geofencingService.esFichajeValido(null, -3.7038, 40.4168, -3.7038, 50);
        assertFalse(resultado, "Si falta cualquier coordenada, debe devolver falso por seguridad");
    }
}
