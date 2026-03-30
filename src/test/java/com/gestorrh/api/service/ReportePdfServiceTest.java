package com.gestorrh.api.service;

import com.gestorrh.api.dto.reporte.ReporteDetalleDTO;
import com.gestorrh.api.dto.reporte.ReporteResumenDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportePdfServiceTest {

    private ReportePdfService reportePdfService;

    @BeforeEach
    void setUp() {
        reportePdfService = new ReportePdfService();
    }

    @Nested
    @DisplayName("Tests para generarPdfDetalle")
    class GenerarPdfDetalleTests {

        @Test
        @DisplayName("Éxito: Generar PDF detallado con múltiples empleados y cambios de departamento")
        void generarPdfDetalle_Exito() {
            // GIVEN
            ReporteDetalleDTO dto1 = ReporteDetalleDTO.builder()
                    .nombreEmpleado("Empleado 1")
                    .departamento("IT")
                    .fecha(LocalDate.of(2026, 3, 1))
                    .descripcionTurno("Mañana")
                    .horaEntradaReal("08:00")
                    .horaSalidaReal("16:00")
                    .tiempoTotalMinutos(480L)
                    .minutosExtra(0L)
                    .build();

            ReporteDetalleDTO dto2 = ReporteDetalleDTO.builder()
                    .nombreEmpleado("Empleado 1")
                    .departamento("IT")
                    .fecha(LocalDate.of(2026, 3, 2))
                    .descripcionTurno("Mañana")
                    .horaEntradaReal("08:15")
                    .horaSalidaReal("16:15")
                    .tiempoTotalMinutos(480L)
                    .minutosExtra(0L)
                    .build();

            ReporteDetalleDTO dto3 = ReporteDetalleDTO.builder()
                    .nombreEmpleado("Empleado 2")
                    .departamento("RRHH")
                    .fecha(LocalDate.of(2026, 3, 1))
                    .descripcionTurno("Tarde")
                    .horaEntradaReal("14:00")
                    .horaSalidaReal("22:00")
                    .tiempoTotalMinutos(480L)
                    .minutosExtra(30L)
                    .build();

            List<ReporteDetalleDTO> datos = Arrays.asList(dto1, dto2, dto3);

            // WHEN
            byte[] pdf = reportePdfService.generarPdfDetalle("Empresa Test", "Desde 01/03 al 02/03", datos);

            // THEN
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
            assertEquals((byte) 0x25, pdf[0]);
            assertEquals((byte) 0x50, pdf[1]);
        }

        @Test
        @DisplayName("Éxito: Generar PDF detallado con lista vacía")
        void generarPdfDetalle_ListaVacia() {
            // WHEN
            byte[] pdf = reportePdfService.generarPdfDetalle("Empresa Test", "Filtro vacío", Collections.emptyList());

            // THEN
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Éxito: Generar PDF detallado con campos nulos en DTOs")
        void generarPdfDetalle_DatosNulos() {
            // GIVEN
            ReporteDetalleDTO dtoNulo = new ReporteDetalleDTO();
            dtoNulo.setNombreEmpleado("Empleado Fantasma");

            List<ReporteDetalleDTO> datos = Collections.singletonList(dtoNulo);

            // WHEN
            byte[] pdf = reportePdfService.generarPdfDetalle("Empresa Test", "Filtro Nulos", datos);

            // THEN
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }
        
        @Test
        @DisplayName("Error: Lanzar RuntimeException si falla la generación")
        void generarPdfDetalle_Error() {
            assertThrows(RuntimeException.class, () -> 
                reportePdfService.generarPdfDetalle("Empresa Test", "Filtro Error", null)
            );
        }
    }

    @Nested
    @DisplayName("Tests para generarPdfResumen")
    class GenerarPdfResumenTests {

        @Test
        @DisplayName("Éxito: Generar PDF resumen con varios empleados")
        void generarPdfResumen_Exito() {
            // GIVEN
            ReporteResumenDTO r1 = ReporteResumenDTO.builder()
                    .nombreEmpleado("Empleado 1")
                    .departamento("IT")
                    .diasTrabajados(20)
                    .totalTiempoTotalMinutos(9600L)
                    .totalMinutosExtra(120L)
                    .build();

            ReporteResumenDTO r2 = ReporteResumenDTO.builder()
                    .nombreEmpleado("Empleado 2")
                    .departamento("Ventas")
                    .diasTrabajados(18)
                    .totalTiempoTotalMinutos(8640L)
                    .totalMinutosExtra(0L)
                    .build();

            List<ReporteResumenDTO> datos = Arrays.asList(r1, r2);

            // WHEN
            byte[] pdf = reportePdfService.generarPdfResumen("Empresa Test", "Resumen Mensual", datos);

            // THEN
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
            assertEquals((byte) 0x25, pdf[0]);
        }

        @Test
        @DisplayName("Éxito: Generar PDF resumen con lista vacía")
        void generarPdfResumen_ListaVacia() {
            // WHEN
            byte[] pdf = reportePdfService.generarPdfResumen("Empresa Test", "Resumen vacío", Collections.emptyList());

            // THEN
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Éxito: Generar PDF resumen con datos nulos")
        void generarPdfResumen_DatosNulos() {
            // GIVEN
            ReporteResumenDTO rNulo = new ReporteResumenDTO();
            rNulo.setNombreEmpleado("Nulo");

            List<ReporteResumenDTO> datos = Collections.singletonList(rNulo);

            // WHEN
            byte[] pdf = reportePdfService.generarPdfResumen("Empresa Test", "Resumen Nulo", datos);

            // THEN
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Error: Lanzar RuntimeException si falla la generación resumen")
        void generarPdfResumen_Error() {
            assertThrows(RuntimeException.class, () ->
                reportePdfService.generarPdfResumen("Empresa Test", "Filtro Error", null)
            );
        }
    }
}
