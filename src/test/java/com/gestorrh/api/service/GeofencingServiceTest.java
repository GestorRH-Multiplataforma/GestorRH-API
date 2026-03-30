package com.gestorrh.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeofencingServiceTest {

    private final GeofencingService geofencingService = new GeofencingService();

    @Nested
    @DisplayName("Tests para esFichajeValido")
    class EsFichajeValidoTests {

        @Test
        @DisplayName("Éxito: Fichaje dentro del radio permitido")
        void esFichajeValido_DentroDelRadio_Exito() {
            // Sede: Puerta del Sol
            Double latSede = 40.4168;
            Double lonSede = -3.7038;
            Integer radioValidez = 100;

            // Empleado: 50 metros de la sede
            Double latEmpleado = 40.4172;
            Double lonEmpleado = -3.7038;

            assertTrue(geofencingService.esFichajeValido(latEmpleado, lonEmpleado, latSede, lonSede, radioValidez));
        }

        @Test
        @DisplayName("Falla: Fichaje fuera del radio permitido")
        void esFichajeValido_FueraDelRadio_Falla() {
            // Sede: Puerta del Sol
            Double latSede = 40.4168;
            Double lonSede = -3.7038;
            Integer radioValidez = 100;

            // Empleado: 500 metros de la sede
            Double latEmpleado = 40.4154;
            Double lonEmpleado = -3.7074;

            assertFalse(geofencingService.esFichajeValido(latEmpleado, lonEmpleado, latSede, lonSede, radioValidez));
        }

        @Test
        @DisplayName("Límite: Fichaje exactamente en el borde del radio")
        void esFichajeValido_EnElBorde_Exito() {
            Double latSede = 0.0;
            Double lonSede = 0.0;

            Integer radioValidez = 1000;

            Double latEmpleado = 0.008993;
            Double lonEmpleado = 0.0;

            assertTrue(geofencingService.esFichajeValido(latEmpleado, lonEmpleado, latSede, lonSede, radioValidez));
        }

        @ParameterizedTest(name = "Caso nulo: latE={0}, lonE={1}, latS={2}, lonS={3}, radio={4}")
        @MethodSource("proveerCasosNulos")
        @DisplayName("Falla: Parámetros nulos")
        void esFichajeValido_ParametrosNulos_Falla(Double latE, Double lonE, Double latS, Double lonS, Integer radio) {
            assertFalse(geofencingService.esFichajeValido(latE, lonE, latS, lonS, radio));
        }

        private static Stream<Arguments> proveerCasosNulos() {
            return Stream.of(
                    Arguments.of(null, -3.7038, 40.4168, -3.7038, 100),
                    Arguments.of(40.4168, null, 40.4168, -3.7038, 100),
                    Arguments.of(40.4168, -3.7038, null, -3.7038, 100),
                    Arguments.of(40.4168, -3.7038, 40.4168, null, 100),
                    Arguments.of(40.4168, -3.7038, 40.4168, -3.7038, null)
            );
        }
    }
}
