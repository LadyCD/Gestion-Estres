package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Detección del nivel de estrés (tabla deteccion) - HU003. Solo el indicador, sin señal cruda. */
@Entity
@Table(name = "deteccion")
public class Deteccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('BAJO','MODERADO','ALTO')")
    private NivelEstres nivel;

    /** Desviación de la HRV respecto a la línea base, en desviaciones estándar. */
    @Column(name = "desviacion_base", columnDefinition = "FLOAT")
    private Float desviacionBase;

    @Column(name = "es_parcial", nullable = false)
    private Boolean esParcial = Boolean.FALSE;

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public NivelEstres getNivel() { return nivel; }
    public void setNivel(NivelEstres nivel) { this.nivel = nivel; }
    public Float getDesviacionBase() { return desviacionBase; }
    public void setDesviacionBase(Float desviacionBase) { this.desviacionBase = desviacionBase; }
    public Boolean getEsParcial() { return esParcial; }
    public void setEsParcial(Boolean esParcial) { this.esParcial = esParcial; }
}
