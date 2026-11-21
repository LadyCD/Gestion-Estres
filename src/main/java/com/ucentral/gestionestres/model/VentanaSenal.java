package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Ventana de 15 minutos enviada por la app (tabla ventana_senal) - HU026.
 * Solo promedios, no la señal cruda. Se eliminan a las 24 horas.
 */
@Entity
@Table(name = "ventana_senal")
public class VentanaSenal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(name = "fc_promedio", nullable = false, columnDefinition = "FLOAT")
    private Float fcPromedio;

    /** NULL si el wearable no entrega HRV. */
    @Column(name = "hrv_promedio", columnDefinition = "FLOAT")
    private Float hrvPromedio;

    @Column(name = "num_lecturas", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private Integer numLecturas;

    @Column(name = "recibida_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime recibidaEn;

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }
    public Float getFcPromedio() { return fcPromedio; }
    public void setFcPromedio(Float fcPromedio) { this.fcPromedio = fcPromedio; }
    public Float getHrvPromedio() { return hrvPromedio; }
    public void setHrvPromedio(Float hrvPromedio) { this.hrvPromedio = hrvPromedio; }
    public Integer getNumLecturas() { return numLecturas; }
    public void setNumLecturas(Integer numLecturas) { this.numLecturas = numLecturas; }
    public LocalDateTime getRecibidaEn() { return recibidaEn; }
}
