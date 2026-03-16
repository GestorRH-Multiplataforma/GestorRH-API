package com.gestorrh.api.dto.empresaDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEmpresaDTO {

    private Long idEmpresa;
    private String email;
    private String nombre;
    private String direccion;
    private String telefono;
    private Double latitudSede;
    private Double longitudSede;
    private Integer radioValidez;
}
