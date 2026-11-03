package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Accesos rechazados (HU020) y consultas del reporte agregado (HU010, HU024). */
@Entity
@Table(name = "registro_acceso")
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 150)
    private String funcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('PERMITIDO','RECHAZADO')")
    private ResultadoAcceso resultado;

    @Column(length = 255)
    private String filtros;

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getFuncion() { return funcion; }
    public void setFuncion(String funcion) { this.funcion = funcion; }
    public ResultadoAcceso getResultado() { return resultado; }
    public void setResultado(ResultadoAcceso resultado) { this.resultado = resultado; }
    public String getFiltros() { return filtros; }
    public void setFiltros(String filtros) { this.filtros = filtros; }
}
