package com.ucentral.gestionestres.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ucentral.gestionestres.model.EstadoDispositivo;

import java.time.LocalDateTime;

/**
 * Estado del dispositivo del usuario (HU002).
 * tokenDispositivo solo se envía en la respuesta de vincular: es la credencial que la app
 * usa para enviar ventanas en segundo plano (HU026). El servidor solo guarda su hash.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DispositivoDTO(
        EstadoDispositivo estado,
        Boolean hrvDisponible,
        LocalDateTime ultimaSincronizacion,
        String tokenDispositivo) {
}
