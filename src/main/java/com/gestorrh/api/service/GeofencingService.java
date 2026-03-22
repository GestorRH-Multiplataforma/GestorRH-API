package com.gestorrh.api.service;

import org.springframework.stereotype.Service;

/**
 * Servicio especializado en cálculos matemáticos para la gestión de geovallado (Geofencing).
 * <p>
 * Proporciona la lógica necesaria para determinar con precisión si una posición geográfica (latitud y longitud)
 * reportada por un dispositivo móvil se encuentra dentro de un radio de acción permitido respecto 
 * a una ubicación de referencia preconfigurada (sede de la empresa).
 * </p>
 * <p>
 * Este servicio es puramente computacional y no realiza operaciones de persistencia o acceso a red.
 * </p>
 */
@Service
public class GeofencingService {

    private static final int RADIO_TIERRA_METROS = 6371000;

    /**
     * Determina si un intento de fichaje es válido basándose en la ubicación GPS del empleado.
     * Compara las coordenadas recibidas con las coordenadas de la sede de la empresa.
     *
     * @param latEmpleado Latitud reportada por el dispositivo del empleado.
     * @param lonEmpleado Longitud reportada por el dispositivo del empleado.
     * @param latSede Latitud configurada de la sede de la empresa.
     * @param lonSede Longitud configurada de la sede de la empresa.
     * @param radioValidez Radio máximo permitido en metros desde el punto de la sede.
     * @return boolean Verdadero si la distancia es menor o igual al radio de validez; falso en caso contrario o si falta algún dato.
     */
    public boolean esFichajeValido(Double latEmpleado, Double lonEmpleado, Double latSede, Double lonSede, Integer radioValidez) {
        if (latEmpleado == null || lonEmpleado == null || latSede == null || lonSede == null || radioValidez == null) {
            return false;
        }

        double distanciaMetros = calcularDistancia(latEmpleado, lonEmpleado, latSede, lonSede);
        return distanciaMetros <= radioValidez;
    }

    /**
     * Calcula la distancia lineal entre dos puntos sobre la superficie terrestre utilizando la fórmula del Haversine.
     * <p>
     * Esta fórmula tiene en cuenta la naturaleza esférica de la Tierra (aproximada por el {@code RADIO_TIERRA_METROS})
     * para obtener una distancia de arco de gran círculo de alta precisión, expresada en metros. 
     * El cálculo es robusto frente a distancias pequeñas, lo cual es ideal para validaciones de geovallado.
     * </p>
     * <p>
     * Pasos del cálculo:
     * </p>
     * <ol>
     *   <li>Conversión de coordenadas (grados decimales) a radianes.</li>
     *   <li>Cálculo de las diferencias de latitud y longitud.</li>
     *   <li>Aplicación de la función seno cuadrado y coseno para obtener el coeficiente {@code a}.</li>
     *   <li>Obtención del ángulo central {@code c} mediante {@code atan2}.</li>
     *   <li>Multiplicación por el radio terrestre para el resultado final.</li>
     * </ol>
     *
     * @param lat1 Latitud del punto de origen (punto A).
     * @param lon1 Longitud del punto de origen (punto A).
     * @param lat2 Latitud del punto de destino (punto B).
     * @param lon2 Longitud del punto de destino (punto B).
     * @return double Distancia absoluta resultante expresada en metros.
     */
    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RADIO_TIERRA_METROS * c;
    }
}
