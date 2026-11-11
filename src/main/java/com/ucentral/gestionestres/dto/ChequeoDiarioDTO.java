package com.ucentral.gestionestres.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChequeoDiarioDTO(
        @NotNull(message = "El ID del usuario es obligatorio")
        Integer usuarioId,

        @NotNull(message = "El puntaje de estrés es obligatorio")
        @Min(value = 0, message = "El puntaje mínimo es 0")
        @Max(value = 10, message = "El puntaje máximo es 10")
        Integer puntaje,

        @Min(value = 1, message = "La energía mínima es 1")
        @Max(value = 5, message = "La energía máxima es 5")
        Integer energia,

        Boolean tension
) {}