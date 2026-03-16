package com.gestorrh.api.service;

import org.springframework.stereotype.Service;

/**
 * Servicio puro de cálculo matemático para la geolocalización.
 * No interactúa con la base de datos, solo computa coordenadas.
 */
@Service
public class GeofencingService {

    private static final int RADIO_TIERRA_METROS = 6371000;

    /**
     * Calcula si unas coordenadas están dentro del radio permitido de una sede.
     */
    public boolean esFichajeValido(Double latEmpleado, Double lonEmpleado, Double latSede, Double lonSede, Integer radioValidez) {
        if (latEmpleado == null || lonEmpleado == null || latSede == null || lonSede == null || radioValidez == null) {
            return false;
        }

        double distanciaMetros = calcularDistancia(latEmpleado, lonEmpleado, latSede, lonSede);
        return distanciaMetros <= radioValidez;
    }

    /**
     * Aplica la fórmula del Haversine para calcular la distancia en metros entre dos puntos GPS.
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
