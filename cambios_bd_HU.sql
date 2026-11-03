-- =====================================================================
-- Cambios a la base de datos gestion_estres derivados de la validación
-- INVEST de las historias de usuario (02/11/2026)
-- Se ejecuta DESPUÉS de proyecto.sql (rama Base). MySQL 8.0.16+
-- =====================================================================
USE gestion_estres;

-- ---------------------------------------------------------------------
-- 1. HU009 · Registro de actividades: tipo, hora de inicio, hora de fin y nota
--    (la tabla solo tenía descripción y fecha_hora)
-- ---------------------------------------------------------------------
ALTER TABLE actividad
  RENAME COLUMN descripcion TO nota,
  RENAME COLUMN fecha_hora  TO creada_en;

ALTER TABLE actividad
  MODIFY COLUMN nota VARCHAR(255) NULL,                     -- la nota es opcional
  ADD COLUMN tipo ENUM('REUNION','ENTREGA_CON_PLAZO','TRABAJO_INDIVIDUAL',
                       'PAUSA_DESCANSO','INTERACCION_SOCIAL','OTRA')
             NOT NULL AFTER usuario_id,
  ADD COLUMN hora_inicio DATETIME NOT NULL AFTER tipo,
  ADD COLUMN hora_fin    DATETIME NULL     AFTER hora_inicio,  -- NULL si solo se indica la duración
  ADD COLUMN duracion_min SMALLINT UNSIGNED NULL AFTER hora_fin,
  ADD CONSTRAINT ck_actividad_fin  CHECK (hora_fin IS NULL OR hora_fin > hora_inicio),
  ADD CONSTRAINT ck_actividad_tiempo CHECK (hora_fin IS NOT NULL OR duracion_min IS NOT NULL),
  DROP INDEX ix_actividad_usuario_fecha,
  ADD INDEX ix_actividad_usuario_inicio (usuario_id, hora_inicio);

-- ---------------------------------------------------------------------
-- 2. HU020 · Bloqueo por 5 intentos fallidos durante 15 minutos
-- ---------------------------------------------------------------------
ALTER TABLE usuario
  ADD COLUMN intentos_fallidos TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER rol,
  ADD COLUMN bloqueado_hasta   DATETIME NULL AFTER intentos_fallidos;

-- ---------------------------------------------------------------------
-- 3. HU020 / HU010 / HU024 · Registro de accesos
--    Accesos rechazados por rol (HU020) y consultas del reporte agregado
--    con sus filtros (HU010, HU024)
-- ---------------------------------------------------------------------
CREATE TABLE registro_acceso (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id  INT UNSIGNED NOT NULL,
  fecha_hora  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  funcion     VARCHAR(150) NOT NULL,    -- p. ej. 'GET /api/reportes/area'
  resultado   ENUM('PERMITIDO','RECHAZADO') NOT NULL,
  filtros     VARCHAR(255) NULL,        -- p. ej. 'area=2;periodo=2026-11'
  PRIMARY KEY (id),
  KEY ix_registro_usuario_fecha (usuario_id, fecha_hora),
  KEY ix_registro_resultado (resultado, fecha_hora),
  CONSTRAINT fk_registro_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. HU021 / HU022 · Autorreporte
--    Diario (HU021): puntaje = pregunta 1 (estrés 0-10) + energía (1-5) + tensión (Sí/No)
--    Mensual (HU022): puntaje PSS-10 de 0 a 40
-- ---------------------------------------------------------------------
ALTER TABLE cuestionario
  ADD COLUMN energia TINYINT UNSIGNED NULL AFTER puntaje,
  ADD COLUMN tension BOOLEAN NULL AFTER energia,
  DROP CHECK ck_cuestionario_puntaje,
  ADD CONSTRAINT ck_cuestionario_rango CHECK (
        (tipo = 'DIARIO'  AND puntaje BETWEEN 0 AND 10
                          AND energia BETWEEN 1 AND 5 AND tension IS NOT NULL)
     OR (tipo = 'MENSUAL' AND puntaje BETWEEN 0 AND 40
                          AND energia IS NULL AND tension IS NULL));

-- ---------------------------------------------------------------------
-- 5. HU023 · Hora del chequeo diario configurada por el usuario
-- ---------------------------------------------------------------------
ALTER TABLE usuario
  ADD COLUMN hora_chequeo TIME NOT NULL DEFAULT '16:00:00' AFTER bloqueado_hasta;
