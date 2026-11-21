package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Actividad registrada por el usuario (tabla actividad) - HU009. */
@Entity
@Table(name = "actividad")
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition =
            "ENUM('REUNION','ENTREGA_CON_PLAZO','TRABAJO_INDIVIDUAL','PAUSA_DESCANSO','INTERACCION_SOCIAL','OTRA')")
    private TipoActividad tipo;

    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    /** Null cuando el usuario indica la duración en lugar de la hora de fin. */
    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    @Column(name = "duracion_min", columnDefinition = "SMALLINT UNSIGNED")
    private Integer duracionMin;

    @Column(length = 255)
    private String nota;

    @Column(name = "creada_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadaEn;

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public TipoActividad getTipo() { return tipo; }
    public void setTipo(TipoActividad tipo) { this.tipo = tipo; }
    public LocalDateTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalDateTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalDateTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalDateTime horaFin) { this.horaFin = horaFin; }
    public Integer getDuracionMin() { return duracionMin; }
    public void setDuracionMin(Integer duracionMin) { this.duracionMin = duracionMin; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
}
