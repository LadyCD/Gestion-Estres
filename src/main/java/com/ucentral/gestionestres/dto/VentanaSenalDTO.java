package com.ucentral.gestionestres.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record VentanaSenalDTO(
        @NotNull(message = "El ID del usuario es obligatorio")
        Integer usuarioId,

        @NotNull(message = "La fecha y hora de inicio es obligatoria")
        LocalDateTime inicio,

        @NotNull(message = "La fecha y hora de fin es obligatoria")
        LocalDateTime fin,

        @NotNull(message = "El promedio de frecuencia cardíaca es obligatorio")
        @Positive(message = "La frecuencia cardíaca debe ser un valor positivo")
        Float fcPromedio,

        Float hrvPromedio,

        @NotNull(message = "El número de lecturas es obligatorio")
        @Positive(message = "El número de lecturas debe ser mayor a cero")
        Integer numLecturas
) {}
