package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Consentimiento informado, Ley 1581 de 2012 (tabla consentimiento) - HU001. */
@Entity
@Table(name = "consentimiento")
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(name = "fecha_aceptacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaAceptacion;

    @Column(nullable = false)
    private Boolean vigente = Boolean.TRUE;

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public LocalDateTime getFechaAceptacion() { return fechaAceptacion; }
    public Boolean getVigente() { return vigente; }
    public void setVigente(Boolean vigente) { this.vigente = vigente; }
}
