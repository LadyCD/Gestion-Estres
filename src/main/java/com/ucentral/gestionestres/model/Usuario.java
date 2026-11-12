package com.ucentral.gestionestres.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** Usuario del sistema (tabla usuario) - HU001, HU020, HU023. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;


    @Column(nullable = false, length = 150)
    private String correo;

    /** HU001: la cuenta solo permite iniciar sesión cuando el correo está verificado. */
    @Column(name = "correo_verificado", nullable = false)
    private Boolean correoVerificado = Boolean.FALSE;

    /** HU001: SHA-256 del código enviado al correo; NULL cuando no hay código pendiente. */
    @Column(name = "codigo_verificacion_hash", columnDefinition = "CHAR(64)")
    private String codigoVerificacionHash;

    @Column(name = "codigo_expira")
    private LocalDateTime codigoExpira;

    @Column(name = "intentos_codigo", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer intentosCodigo = 0;

    /** Hash BCrypt; nunca la contraseña en texto plano. */
    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('TRABAJADOR','SST_TH')")
    private Rol rol = Rol.TRABAJADOR;

    @Column(name = "intentos_fallidos", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "hora_chequeo", nullable = false)
    private LocalTime horaChequeo = LocalTime.of(16, 0);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cargo_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Area area;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public Boolean getCorreoVerificado() { return correoVerificado; }
    public void setCorreoVerificado(Boolean correoVerificado) { this.correoVerificado = correoVerificado; }
    public String getCodigoVerificacionHash() { return codigoVerificacionHash; }
    public void setCodigoVerificacionHash(String codigoVerificacionHash) { this.codigoVerificacionHash = codigoVerificacionHash; }
    public LocalDateTime getCodigoExpira() { return codigoExpira; }
    public void setCodigoExpira(LocalDateTime codigoExpira) { this.codigoExpira = codigoExpira; }
    public Integer getIntentosCodigo() { return intentosCodigo; }
    public void setIntentosCodigo(Integer intentosCodigo) { this.intentosCodigo = intentosCodigo; }
    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Integer getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(Integer intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public LocalTime getHoraChequeo() { return horaChequeo; }
    public void setHoraChequeo(LocalTime horaChequeo) { this.horaChequeo = horaChequeo; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
