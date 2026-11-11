package com.ucentral.gestionestres.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * POST /api/ventanas (HU026): promedios de una ventana de 15 minutos, no la señal cruda.
 * El usuario no viaja en el cuerpo: se identifica con la credencial del dispositivo.
 */
public record VentanaSenalDTO(
        @NotNull(message = "La fecha y hora de inicio es obligatoria")
        LocalDateTime inicio,

        @NotNull(message = "La fecha y hora de fin es obligatoria")
        LocalDateTime fin,

        @NotNull(message = "El promedio de frecuencia cardíaca es obligatorio")
        @Positive(message = "La frecuencia cardíaca debe ser un valor positivo")
        Float fcPromedio,

        /** Null si el wearable no entrega HRV. */
        @Positive(message = "La HRV debe ser un valor positivo")
        Float hrvPromedio,

        @NotNull(message = "El número de lecturas es obligatorio")
        @Positive(message = "El número de lecturas debe ser mayor a cero")
        Integer numLecturas
) {}
