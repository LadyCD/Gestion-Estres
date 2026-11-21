package com.ucentral.gestionestres.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/cuestionarios/diario (HU021): chequeo breve de 3 preguntas.
 * El usuario no viaja en la petición: se toma de la sesión (JWT, HU020).
 */
public record ChequeoDiarioDTO(
        @NotNull(message = "Responda qué tan estresado(a) se siente (0 a 10)")
        @Min(value = 0, message = "El estrés mínimo es 0")
        @Max(value = 10, message = "El estrés máximo es 10")
        Integer estres,

        @NotNull(message = "Responda su nivel de energía (1 a 5)")
        @Min(value = 1, message = "La energía mínima es 1")
        @Max(value = 5, message = "La energía máxima es 5")
        Integer energia,

        @NotNull(message = "Responda si hay una situación que le genera tensión")
        Boolean tension
) {}
