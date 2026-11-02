-- =====================================================================
-- Base de datos: Sistema de monitoreo de estrés laboral
-- Motor: MySQL 8.0.16+ (necesario para que los CHECK se apliquen)
-- Basada en el modelo entidad-relación (sección 9.4 del documento)
--
-- Decisiones de diseño:
--  * Privacidad desde el diseño: NO existe tabla de mediciones crudas.
--    Solo se guarda el indicador derivado (nivel y desviación a la línea base).
--  * Los reportes agregados se relacionan con el área, nunca con usuarios.
--  * Derecho de supresión (Ley 1581 de 2012): al borrar un usuario se borran
--    en cascada sus datos personales y de salud.

CREATE DATABASE IF NOT EXISTS gestion_estres
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE gestion_estres;

-- ---------------------------------------------------------------------
-- Catálogos
-- ---------------------------------------------------------------------
CREATE TABLE area (
  id      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre  VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_area_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE cargo (
  id      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre  VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_cargo_nombre (nombre)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Usuarios (HU001)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
  id               INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre_completo  VARCHAR(150) NOT NULL,
  documento        VARCHAR(20)  NOT NULL,
  correo           VARCHAR(150) NOT NULL,
  contrasena_hash  VARCHAR(255) NOT NULL,   -- hash (p. ej. BCrypt), nunca la contraseña
  rol              ENUM('TRABAJADOR','SST_TH') NOT NULL DEFAULT 'TRABAJADOR',
  cargo_id         INT UNSIGNED NOT NULL,
  area_id          INT UNSIGNED NOT NULL,
  creado_en        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_usuario_documento (documento),
  UNIQUE KEY uq_usuario_correo (correo),
  CONSTRAINT ck_usuario_documento CHECK (documento REGEXP '^[0-9]+$'),
  CONSTRAINT ck_usuario_correo CHECK (correo REGEXP '^[^@ ]+@[^@ ]+[.][^@ ]+$'),
  CONSTRAINT fk_usuario_cargo FOREIGN KEY (cargo_id) REFERENCES cargo (id) ON DELETE RESTRICT,
  CONSTRAINT fk_usuario_area  FOREIGN KEY (area_id)  REFERENCES area (id)  ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Consentimiento informado, Ley 1581 de 2012 (HU001)
CREATE TABLE consentimiento (
  id                 INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id         INT UNSIGNED NOT NULL,
  version            VARCHAR(20)  NOT NULL,
  fecha_aceptacion   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  vigente            BOOLEAN      NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  KEY ix_consentimiento_usuario (usuario_id, vigente),
  CONSTRAINT fk_consentimiento_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Dispositivo vinculado vía Health Connect (HU002)
-- Un usuario tiene como máximo un dispositivo (relación 1 a 0..1)
-- ---------------------------------------------------------------------
CREATE TABLE dispositivo_vinculado (
  id                      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id              INT UNSIGNED NOT NULL,
  plataforma              VARCHAR(50)  NOT NULL DEFAULT 'Health Connect',
  estado                  ENUM('VINCULADO','DESCONECTADO') NOT NULL DEFAULT 'DESCONECTADO',
  ultima_sincronizacion   DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dispositivo_usuario (usuario_id),
  CONSTRAINT fk_dispositivo_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Autorreporte: chequeo diario y escala mensual (HU021)
-- ---------------------------------------------------------------------
CREATE TABLE cuestionario (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id  INT UNSIGNED NOT NULL,
  tipo        ENUM('DIARIO','MENSUAL') NOT NULL,
  puntaje     INT          NOT NULL,
  fecha_hora  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_cuestionario_usuario_fecha (usuario_id, fecha_hora),
  CONSTRAINT ck_cuestionario_puntaje CHECK (puntaje >= 0),
  CONSTRAINT fk_cuestionario_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Línea base individual (HU003). Un registro por usuario.
-- ---------------------------------------------------------------------
CREATE TABLE linea_base (
  id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id      INT UNSIGNED NOT NULL,
  fc_base         FLOAT        NOT NULL,   -- frecuencia cardíaca base (lpm)
  hrv_base        FLOAT        NOT NULL,   -- variabilidad de la FC base (ms)
  fecha_calculo   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_linea_base_usuario (usuario_id),
  CONSTRAINT fk_linea_base_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Detección de estrés (HU003): solo el indicador derivado, sin señal cruda
-- ---------------------------------------------------------------------
CREATE TABLE deteccion (
  id               INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id       INT UNSIGNED NOT NULL,
  fecha_hora       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  nivel            ENUM('BAJO','MODERADO','ALTO') NOT NULL,
  desviacion_base  FLOAT        NULL,      -- desviación respecto a la línea base
  es_parcial       BOOLEAN      NOT NULL DEFAULT FALSE,  -- TRUE si el wearable no entregó datos
  PRIMARY KEY (id),
  KEY ix_deteccion_usuario_fecha (usuario_id, fecha_hora),
  KEY ix_deteccion_fecha (fecha_hora),
  CONSTRAINT fk_deteccion_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Alertas (HU004). Una detección dispara como máximo una alerta (1 a 0..1)
-- ---------------------------------------------------------------------
CREATE TABLE alerta (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  deteccion_id  INT UNSIGNED NOT NULL,
  creada_en     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sugerencia    VARCHAR(255) NOT NULL,
  estado        ENUM('GENERADA','ENVIADA','DESCARTADA','POSPUESTA','VALORADA')
                NOT NULL DEFAULT 'GENERADA',
  util          BOOLEAN      NULL,         -- NULL hasta que el usuario la valore
  PRIMARY KEY (id),
  UNIQUE KEY uq_alerta_deteccion (deteccion_id),
  CONSTRAINT fk_alerta_deteccion FOREIGN KEY (deteccion_id)
    REFERENCES deteccion (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Personalización de sugerencias y registro de actividades
-- ---------------------------------------------------------------------
CREATE TABLE preferencia_sugerencia (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id  INT UNSIGNED NOT NULL,
  tipo        VARCHAR(50)  NOT NULL,       -- p. ej. 'pausa activa', 'respiración'
  activa      BOOLEAN      NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  UNIQUE KEY uq_preferencia_usuario_tipo (usuario_id, tipo),
  CONSTRAINT fk_preferencia_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE actividad (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id   INT UNSIGNED NOT NULL,
  descripcion  VARCHAR(255) NOT NULL,
  fecha_hora   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_actividad_usuario_fecha (usuario_id, fecha_hora),
  CONSTRAINT fk_actividad_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Reportes agregados y anónimos para SST/Talento Humano (HU010)
-- Se relaciona con el área, nunca con un usuario. El umbral mínimo de
-- participantes (5) es un supuesto: ajustarlo con el equipo de SST.
-- ---------------------------------------------------------------------
CREATE TABLE reporte_agregado (
  id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  area_id         INT UNSIGNED NOT NULL,
  periodo         VARCHAR(20)  NOT NULL,   -- p. ej. '2026-11'
  nivel_promedio  FLOAT        NOT NULL,   -- 1 = bajo, 2 = moderado, 3 = alto
  participantes   INT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_reporte_area_periodo (area_id, periodo),
  CONSTRAINT ck_reporte_anonimato CHECK (participantes >= 5),
  CONSTRAINT fk_reporte_area FOREIGN KEY (area_id) REFERENCES area (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Vista de apoyo: nivel promedio por área y mes, solo si hay al menos
-- 5 trabajadores distintos (protege el anonimato).
-- ---------------------------------------------------------------------
CREATE VIEW v_nivel_por_area_mes AS
SELECT
  u.area_id,
  DATE_FORMAT(d.fecha_hora, '%Y-%m') AS periodo,
  AVG(CASE d.nivel WHEN 'BAJO' THEN 1 WHEN 'MODERADO' THEN 2 ELSE 3 END) AS nivel_promedio,
  COUNT(DISTINCT d.usuario_id) AS participantes
FROM deteccion d
JOIN usuario u ON u.id = d.usuario_id
WHERE u.rol = 'TRABAJADOR'
GROUP BY u.area_id, DATE_FORMAT(d.fecha_hora, '%Y-%m')
HAVING COUNT(DISTINCT d.usuario_id) >= 5;

-- ---------------------------------------------------------------------
-- Reglas de negocio en la base de datos
-- (si usas un cliente que no entiende DELIMITER, ejecuta cada trigger por separado)
-- ---------------------------------------------------------------------
DELIMITER $$

-- HU002: la vinculación solo se habilita si el usuario aceptó el consentimiento
CREATE TRIGGER trg_dispositivo_requiere_consentimiento
BEFORE INSERT ON dispositivo_vinculado
FOR EACH ROW
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM consentimiento
    WHERE usuario_id = NEW.usuario_id AND vigente = TRUE
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'El usuario debe aceptar el consentimiento informado antes de vincular un dispositivo';
  END IF;
END$$

-- HU004: solo se genera alerta cuando la detección es de nivel ALTO
CREATE TRIGGER trg_alerta_solo_nivel_alto
BEFORE INSERT ON alerta
FOR EACH ROW
BEGIN
  DECLARE v_nivel VARCHAR(10);
  SELECT nivel INTO v_nivel FROM deteccion WHERE id = NEW.deteccion_id;
  IF v_nivel IS NULL OR v_nivel <> 'ALTO' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Solo se puede generar una alerta para detecciones de nivel ALTO';
  END IF;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------
-- Datos iniciales de ejemplo (reemplazar por los de la empresa)
-- ---------------------------------------------------------------------
INSERT INTO area (nombre) VALUES
  ('Administrativa'), ('Operaciones'), ('Talento Humano'),
  ('Seguridad y Salud en el Trabajo'), ('Tecnología');

INSERT INTO cargo (nombre) VALUES
  ('Auxiliar'), ('Analista'), ('Coordinador'), ('Jefe de área'), ('Director');
  

  