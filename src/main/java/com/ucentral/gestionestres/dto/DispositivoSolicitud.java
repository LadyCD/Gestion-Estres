package com.ucentral.gestionestres.dto;

import com.ucentral.gestionestres.model.EstadoDispositivo;
import jakarta.validation.constraints.NotNull;

/** PUT /api/dispositivo (HU002): vincular o desvincular el wearable. */
public record DispositivoSolicitud(
        @NotNull(message = "Indique el estado: VINCULADO o DESCONECTADO") EstadoDispositivo estado,
        boolean hrvDisponible) {
}
