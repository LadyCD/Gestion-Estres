package com.ucentral.gestionestres.config;

import com.ucentral.gestionestres.security.JwtFiltro;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Seguridad con JWT sin sesión (HU001 registro abierto, HU020 login y roles). */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Rutas abiertas sin iniciar sesión. */
    private static final String[] RUTAS_PUBLICAS = {
            "/api/auth/login",
            "/api/catalogos/**",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFiltro jwtFiltro) throws Exception {
        http
                .csrf(csrf -> csrf.disable())   // API REST sin sesión de navegador
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        // HU026: la app envía con la credencial del dispositivo (X-Token-Dispositivo),
                        // no con la sesión; la valida VentanaSenalService
                        .requestMatchers(HttpMethod.POST, "/api/ventanas").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401)))
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** HU020: las contraseñas se guardan únicamente como hash BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}