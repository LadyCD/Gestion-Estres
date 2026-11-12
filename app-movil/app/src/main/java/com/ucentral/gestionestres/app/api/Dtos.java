package com.ucentral.gestionestres.app.api;

/**
 * Objetos que viajan entre la app y el backend (JSON).
 * Deben coincidir con el contrato de la API descrito en app-movil/README.md.
 * Las fechas viajan como texto ISO-8601 en hora de Colombia, por ejemplo "2026-11-09T08:15:00".
 */
public final class Dtos {

    private Dtos() { }

    /** GET /api/catalogos/areas y /api/catalogos/cargos */
    public static class Catalogo {
        public Integer id;
        public String nombre;

        @Override
        public String toString() {   // lo usa el Spinner para mostrar el nombre
            return nombre;
        }
    }

    /** POST /api/usuarios (HU001) */
    public static class RegistroSolicitud {
        public String nombreCompleto;
        public String correo;
        public String contrasena;
        public Integer cargoId;
        public Integer areaId;
        public String versionConsentimiento;
    }

    public static class Usuario {
        public Integer id;
        public String nombreCompleto;
        public String correo;
        public String rol;
    }

    /** POST /api/usuarios/verificar (HU001) */
    public static class VerificacionSolicitud {
        public String correo;
        public String codigo;

        public VerificacionSolicitud(String correo, String codigo) {
            this.correo = correo;
            this.codigo = codigo;
        }
    }

    /** POST /api/usuarios/reenviar-codigo (HU001) */
    public static class CorreoSolicitud {
        public String correo;

        public CorreoSolicitud(String correo) {
            this.correo = correo;
        }
    }

    /** POST /api/auth/login (HU020) */
    public static class LoginSolicitud {
        public String correo;
        public String contrasena;

        public LoginSolicitud(String correo, String contrasena) {
            this.correo = correo;
            this.contrasena = contrasena;
        }
    }

    public static class LoginRespuesta {
        public String token;
        public String rol;              // TRABAJADOR o SST_TH
        public String nombreCompleto;
        public Integer expiraEnMinutos;
    }

    /** POST /api/cuestionarios/diario (HU021) */
    public static class ChequeoSolicitud {
        public int estres;      // 0 a 10
        public int energia;     // 1 a 5
        public boolean tension;

        public ChequeoSolicitud(int estres, int energia, boolean tension) {
            this.estres = estres;
            this.energia = energia;
            this.tension = tension;
        }
    }

    /** GET y PUT /api/dispositivo (HU002) */
    public static class DispositivoSolicitud {
        public String estado;           // VINCULADO o DESCONECTADO
        public boolean hrvDisponible;

        public DispositivoSolicitud(String estado, boolean hrvDisponible) {
            this.estado = estado;
            this.hrvDisponible = hrvDisponible;
        }
    }

    public static class Dispositivo {
        public String estado;
        public Boolean hrvDisponible;
        public String ultimaSincronizacion;
        /** Solo llega al vincular: credencial del teléfono para enviar ventanas (HU026). */
        public String tokenDispositivo;
    }

    /** POST /api/ventanas (HU026) */
    public static class VentanaSolicitud {
        public String inicio;
        public String fin;
        public double fcPromedio;
        public Double hrvPromedio;      // null si el wearable no entrega HRV
        public int numLecturas;
    }

    /** GET /api/linea-base (HU025) */
    public static class LineaBase {
        public String estado;           // EN_CALCULO o LISTA
        public int diasValidos;
        public int diasRequeridos;
    }

    /** GET /api/detecciones/ultima (HU003) */
    public static class Deteccion {
        public String fechaHora;
        public String nivel;            // BAJO, MODERADO o ALTO
        public boolean parcial;
    }

    /** Formato de error del backend (exception/ErrorRespuesta.java) */
    public static class ErrorRespuesta {
        public String fecha;
        public int estado;
        public String mensaje;
        public java.util.Map<String, String> campos;
    }
}
