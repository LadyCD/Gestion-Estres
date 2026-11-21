package com.ucentral.gestionestres.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFiltro extends OncePerRequestFilter {

    private final JwtServicio jwt;

    public JwtFiltro(JwtServicio jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            jwt.validar(cabecera.substring(7)).ifPresent(claims -> {
                var autoridad = new SimpleGrantedAuthority("ROLE_" + jwt.rolDe(claims).name());
                var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, List.of(autoridad));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        // Token ausente, inválido o vencido: sigue sin autenticar y Security responde 401
        chain.doFilter(request, response);
    }
}