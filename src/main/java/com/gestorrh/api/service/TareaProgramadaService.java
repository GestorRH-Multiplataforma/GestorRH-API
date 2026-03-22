package com.gestorrh.api.service;

import com.gestorrh.api.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de la ejecución de procesos automáticos y tareas de mantenimiento en segundo plano.
 * <p>
 * Utiliza las capacidades de programación de Spring ({@code @Scheduled}) y la escucha de eventos del 
 * ciclo de vida de la aplicación para asegurar la integridad de los datos de los empleados.
 * </p>
 * <p>
 * Este servicio no es invocado por controladores externos; su ejecución es disparada automáticamente
 * por el contenedor de Spring.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TareaProgramadaService {

    private final EmpleadoRepository empleadoRepository;

    /**
     * Realiza la actualización masiva del estado de los empleados basándose en la vigencia de sus contratos.
     * <p>
     * Esta tarea se ejecuta automáticamente bajo dos condiciones fundamentales:
     * </p>
     * <ul>
     *   <li><b>Ejecución Diaria:</b> Programada a las 00:01 AM (Cron: "0 1 0 * * ?") para procesar bajas del día anterior.</li>
     *   <li><b>Arranque del Sistema:</b> Se dispara al recibir el {@link ApplicationReadyEvent}, asegurando que el estado sea coherente tras un reinicio.</li>
     * </ul>
     * <p>
     * El proceso busca todos los empleados cuya fecha de baja de contrato sea igual o anterior a la fecha actual
     * y los marca como inactivos ({@code activo = false}) en una sola operación de base de datos para optimizar el rendimiento.
     * </p>
     */
    @Transactional
    @Scheduled(cron = "0 1 0 * * ?")
    @EventListener(ApplicationReadyEvent.class)
    public void actualizarEstadoEmpleados() {
        log.info("[CRON] Iniciando tarea de mantenimiento: Revisión de contratos expirados...");

        int actualizados = empleadoRepository.desactivarEmpleadosConContratoExpirado();

        if (actualizados > 0) {
            log.info("[CRON] Mantenimiento completado: Se han desactivado {} empleados con contrato expirado.", actualizados);
        } else {
            log.info("[CRON] Mantenimiento completado: No se han detectado contratos expirados en el día de hoy.");
        }
    }
}
