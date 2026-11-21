package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Wearable vinculado vía Health Connect (tabla dispositivo_vinculado) - HU002, HU026. */
@Entity
@Table(name = "dispositivo_vinculado")
public class DispositivoVinculado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true, columnDefinition = "INT UNSIGNED")
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String plataforma = "Health Connect";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('VINCULADO','DESCONECTADO')")
    private EstadoDispositivo estado = EstadoDispositivo.DESCONECTADO;

    @Column(name = "hrv_disponible", nullable = false)
    private Boolean hrvDisponible = Boolean.FALSE;

    /** Hash SHA-256 de la credencial del teléfono (HU026). NULL si está desvinculado. */
    @Column(name = "token_hash", columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(name = "ultima_sincronizacion")
    private LocalDateTime ultimaSincronizacion;

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }
    public EstadoDispositivo getEstado() { return estado; }
    public void setEstado(EstadoDispositivo estado) { this.estado = estado; }
    public Boolean getHrvDisponible() { return hrvDisponible; }
    public void setHrvDisponible(Boolean hrvDisponible) { this.hrvDisponible = hrvDisponible; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public LocalDateTime getUltimaSincronizacion() { return ultimaSincronizacion; }
    public void setUltimaSincronizacion(LocalDateTime ultimaSincronizacion) { this.ultimaSincronizacion = ultimaSincronizacion; }
}
