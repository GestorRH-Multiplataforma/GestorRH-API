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
 * Servicio encargado de ejecutar procesos automáticos en segundo plano.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TareaProgramadaService {

    private final EmpleadoRepository empleadoRepository;

    /**
     * Se ejecuta todos los días a las 00:01 (Cron) y cada vez que se arranca la API (EventListener).
     * Busca empleados cuyo contrato ha expirado y los marca como inactivos en la BD.
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
