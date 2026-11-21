package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Autorreporte (tabla cuestionario).
 * DIARIO (HU021): puntaje = estrés 0-10, energia 1-5, tension Sí/No.
 * MENSUAL (HU022): puntaje PSS-10 de 0 a 40; energia y tension en null.
 */
@Entity
@Table(name = "cuestionario")
public class Cuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('DIARIO','MENSUAL')")
    private TipoCuestionario tipo;

    @Column(nullable = false)
    private Integer puntaje;

    @Column(columnDefinition = "TINYINT UNSIGNED")
    private Integer energia;

    private Boolean tension;

    @Column(name = "fecha_hora", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaHora;

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public TipoCuestionario getTipo() { return tipo; }
    public void setTipo(TipoCuestionario tipo) { this.tipo = tipo; }
    public Integer getPuntaje() { return puntaje; }
    public void setPuntaje(Integer puntaje) { this.puntaje = puntaje; }
    public Integer getEnergia() { return energia; }
    public void setEnergia(Integer energia) { this.energia = energia; }
    public Boolean getTension() { return tension; }
    public void setTension(Boolean tension) { this.tension = tension; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
