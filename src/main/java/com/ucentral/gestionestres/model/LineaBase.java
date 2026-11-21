package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Línea base individual (tabla linea_base) - HU025.
 * Se actualiza de forma incremental (algoritmo de Welford): n, promedio y M2 por variable.
 * Las desviaciones estándar son columnas generadas por MySQL (solo lectura).
 */
@Entity
@Table(name = "linea_base")
public class LineaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true, columnDefinition = "INT UNSIGNED")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('EN_CALCULO','LISTA')")
    private EstadoLineaBase estado = EstadoLineaBase.EN_CALCULO;

    @Column(name = "dias_validos", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer diasValidos = 0;

    @Column(name = "dia_en_curso")
    private LocalDate diaEnCurso;

    @Column(name = "ventanas_dia_en_curso", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private Integer ventanasDiaEnCurso = 0;

    @Column(name = "fc_n", nullable = false, columnDefinition = "INT UNSIGNED")
    private Integer fcN = 0;

    @Column(name = "fc_base", nullable = false, columnDefinition = "FLOAT")
    private Float fcBase = 0f;

    @Column(name = "fc_m2", nullable = false)
    private Double fcM2 = 0d;

    @Column(name = "hrv_n", nullable = false, columnDefinition = "INT UNSIGNED")
    private Integer hrvN = 0;

    @Column(name = "hrv_base", columnDefinition = "FLOAT")
    private Float hrvBase;

    @Column(name = "hrv_m2", nullable = false)
    private Double hrvM2 = 0d;

    @Column(name = "fc_desv", insertable = false, updatable = false)
    private Double fcDesv;

    @Column(name = "hrv_desv", insertable = false, updatable = false)
    private Double hrvDesv;

    @Column(name = "fecha_calculo")
    private LocalDateTime fechaCalculo;

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public EstadoLineaBase getEstado() { return estado; }
    public void setEstado(EstadoLineaBase estado) { this.estado = estado; }
    public Integer getDiasValidos() { return diasValidos; }
    public void setDiasValidos(Integer diasValidos) { this.diasValidos = diasValidos; }
    public LocalDate getDiaEnCurso() { return diaEnCurso; }
    public void setDiaEnCurso(LocalDate diaEnCurso) { this.diaEnCurso = diaEnCurso; }
    public Integer getVentanasDiaEnCurso() { return ventanasDiaEnCurso; }
    public void setVentanasDiaEnCurso(Integer ventanasDiaEnCurso) { this.ventanasDiaEnCurso = ventanasDiaEnCurso; }
    public Integer getFcN() { return fcN; }
    public void setFcN(Integer fcN) { this.fcN = fcN; }
    public Float getFcBase() { return fcBase; }
    public void setFcBase(Float fcBase) { this.fcBase = fcBase; }
    public Double getFcM2() { return fcM2; }
    public void setFcM2(Double fcM2) { this.fcM2 = fcM2; }
    public Integer getHrvN() { return hrvN; }
    public void setHrvN(Integer hrvN) { this.hrvN = hrvN; }
    public Float getHrvBase() { return hrvBase; }
    public void setHrvBase(Float hrvBase) { this.hrvBase = hrvBase; }
    public Double getHrvM2() { return hrvM2; }
    public void setHrvM2(Double hrvM2) { this.hrvM2 = hrvM2; }
    /** Calculada por MySQL; se actualiza al volver a leer la entidad. */
    public Double getFcDesv() { return fcDesv; }
    public Double getHrvDesv() { return hrvDesv; }
    public LocalDateTime getFechaCalculo() { return fechaCalculo; }
    public void setFechaCalculo(LocalDateTime fechaCalculo) { this.fechaCalculo = fechaCalculo; }
}
