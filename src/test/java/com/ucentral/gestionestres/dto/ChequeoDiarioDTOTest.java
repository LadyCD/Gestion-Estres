package com.ucentral.gestionestres.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** HU021: rangos de las 3 preguntas. */
class ChequeoDiarioDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Respuestas dentro de rango son válidas")
    void valido() {
        assertThat(validador.validate(new ChequeoDiarioDTO(0, 5, false))).isEmpty();
        assertThat(validador.validate(new ChequeoDiarioDTO(10, 1, true))).isEmpty();
    }

    @Test
    @DisplayName("Estrés fuera de 0-10, energía fuera de 1-5 o respuestas vacías se rechazan")
    void invalido() {
        assertThat(validador.validate(new ChequeoDiarioDTO(11, 3, false))).hasSize(1);
        assertThat(validador.validate(new ChequeoDiarioDTO(5, 0, false))).hasSize(1);
        assertThat(validador.validate(new ChequeoDiarioDTO(null, null, null))).hasSize(3);
    }
}
