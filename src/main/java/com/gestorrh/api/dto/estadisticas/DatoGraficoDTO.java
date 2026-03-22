package com.gestorrh.api.dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Universal para alimentar cualquier tipo de gráfico en el Frontend (Barras, Sectores, Líneas).
 * Se compone únicamente de un eje X (etiqueta) y un eje Y (valor).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatoGraficoDTO {

    private String etiqueta;
    private Number valor;

}
