package com.ucentral.gestionestres.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos que muestra Swagger en http://localhost:8080/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("Gestión del Estrés Laboral - API")
                .version("0.1.0")
                .description("Backend de la herramienta para medir y prevenir el estrés laboral. "
                        + "Práctica de Ingeniería III - Universidad Central, Grupo 2."));
    }
}
