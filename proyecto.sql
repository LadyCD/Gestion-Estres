-- =====================================================================
-- Base de datos: Sistema de monitoreo de estrés laboral
-- Motor: MySQL 8.0.16+ (necesario para que los CHECK se apliquen)
-- Script único de creación desde cero (consolida proyecto.sql + cambios
-- derivados de la validación INVEST, HU001-HU026).
--
-- Decisiones de diseño:
--  * Privacidad desde el diseño: NO existe tabla de señal cruda. Solo se
--    guardan ventanas de 15 min con promedios (24 h) y el indicador derivado.
--  * Los reportes agregados se relacionan con el área, nunca con usuarios.
--  * Derecho de supresión (Ley 1581 de 2012): al borrar un usuario se borran
--    en cascada sus datos personales y de salud.
--  * Minimización de datos: no se almacena el documento de identidad.
-- =====================================================================

-- ATENCIÓN: descomentar solo si quiere borrar la base existente.
-- DROP DATABASE IF EXISTS gestion_estres;

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
-- Usuarios (HU001, HU020, HU023)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
  id                 INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre_completo    VARCHAR(150) NOT NULL,
  correo             VARCHAR(150) NOT NULL,          -- correo institucional: identifica al usuario
  contrasena_hash    VARCHAR(255) NOT NULL,          -- hash (p. ej. BCrypt), nunca la contraseña
  rol                ENUM('TRABAJADOR','SST_TH') NOT NULL DEFAULT 'TRABAJADOR',
  intentos_fallidos  TINYINT UNSIGNED NOT NULL DEFAULT 0,   -- HU020: bloqueo tras 5 intentos
  bloqueado_hasta    DATETIME NULL,                         -- HU020: 15 minutos de bloqueo
  hora_chequeo       TIME NOT NULL DEFAULT '16:00:00',      -- HU023: hora del chequeo diario
  cargo_id           INT UNSIGNED NOT NULL,
  area_id            INT UNSIGNED NOT NULL,
  creado_en          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_usuario_correo (correo),
  CONSTRAINT ck_usuario_correo CHECK (correo REGEXP '^[^@ ]+@[^@ ]+[.][^@ ]+$'),
  CONSTRAINT fk_usuario_cargo FOREIGN KEY (cargo_id) REFERENCES cargo (id) ON DELETE RESTRICT,
  CONSTRAINT fk_usuario_area  FOREIGN KEY (area_id)  REFERENCES area (id)  ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Consentimiento informado, Ley 1581 de 2012 (HU001)
CREATE TABLE consentimiento (
  id                INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id        INT UNSIGNED NOT NULL,
  version           VARCHAR(20)  NOT NULL,
  fecha_aceptacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  vigente           BOOLEAN      NOT NULL DEFAULT TRUE,
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
  id                     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id             INT UNSIGNED NOT NULL,
  plataforma             VARCHAR(50)  NOT NULL DEFAULT 'Health Connect',
  estado                 ENUM('VINCULADO','DESCONECTADO') NOT NULL DEFAULT 'DESCONECTADO',
  hrv_disponible         BOOLEAN      NOT NULL DEFAULT FALSE,  -- si es FALSE, la detección (HU003) es parcial
  ultima_sincronizacion  DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dispositivo_usuario (usuario_id),
  CONSTRAINT fk_dispositivo_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Autorreporte (HU021, HU022)
--   DIARIO : puntaje = estrés 0-10; energía 1-5; tensión Sí/No (obligatorias)
--   MENSUAL: puntaje PSS-10 de 0 a 40; energía y tensión deben ser NULL
-- ---------------------------------------------------------------------
CREATE TABLE cuestionario (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id  INT UNSIGNED NOT NULL,
  tipo        ENUM('DIARIO','MENSUAL') NOT NULL,
  puntaje     INT          NOT NULL,
  energia     TINYINT UNSIGNED NULL,
  tension     BOOLEAN      NULL,
  fecha_hora  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_cuestionario_usuario_fecha (usuario_id, fecha_hora),
  CONSTRAINT ck_cuestionario_rango CHECK (
        (tipo = 'DIARIO'  AND puntaje BETWEEN 0 AND 10
                          AND energia IS NOT NULL AND energia BETWEEN 1 AND 5
                          AND tension IS NOT NULL)
     OR (tipo = 'MENSUAL' AND puntaje BETWEEN 0 AND 40
                          AND energia IS NULL AND tension IS NULL)),
  CONSTRAINT fk_cuestionario_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Ventanas de 15 minutos enviadas por la app (HU026): promedios, no la
-- señal cruda. Se conservan 24 horas; el backend las elimina con una tarea
-- programada. La clave única evita ventanas duplicadas.
-- ---------------------------------------------------------------------
CREATE TABLE ventana_senal (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id    INT UNSIGNED  NOT NULL,
  inicio        DATETIME      NOT NULL,
  fin           DATETIME      NOT NULL,
  fc_promedio   FLOAT         NOT NULL,            -- lpm
  hrv_promedio  FLOAT         NULL,                -- ms (RMSSD); NULL si no hay HRV
  num_lecturas  SMALLINT UNSIGNED NOT NULL,
  recibida_en   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ventana_usuario_inicio (usuario_id, inicio),
  KEY ix_ventana_recibida (recibida_en),
  CONSTRAINT ck_ventana_fin      CHECK (fin > inicio),
  CONSTRAINT ck_ventana_fc       CHECK (fc_promedio BETWEEN 30 AND 220),
  CONSTRAINT ck_ventana_hrv      CHECK (hrv_promedio IS NULL OR hrv_promedio > 0),
  CONSTRAINT ck_ventana_lecturas CHECK (num_lecturas > 0),
  CONSTRAINT fk_ventana_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Línea base individual, incremental con el algoritmo de Welford
-- (HU003, HU025). Un registro por usuario.
-- Se guardan n, promedio y suma de cuadrados (M2) de cada variable; la
-- desviación estándar es una columna generada. Pasa a LISTA al completar
-- 5 días con al menos 16 ventanas con FC.
-- ---------------------------------------------------------------------
CREATE TABLE linea_base (
  id                     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id             INT UNSIGNED NOT NULL,
  estado                 ENUM('EN_CALCULO','LISTA') NOT NULL DEFAULT 'EN_CALCULO',
  dias_validos           TINYINT UNSIGNED NOT NULL DEFAULT 0,
  dia_en_curso           DATE NULL,
  ventanas_dia_en_curso  SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  fc_n                   INT UNSIGNED NOT NULL DEFAULT 0,
  fc_base                FLOAT  NOT NULL DEFAULT 0,        -- promedio de FC (lpm)
  fc_m2                  DOUBLE NOT NULL DEFAULT 0,
  hrv_n                  INT UNSIGNED NOT NULL DEFAULT 0,
  hrv_base               FLOAT  NULL,                      -- promedio de HRV (ms); NULL sin HRV
  hrv_m2                 DOUBLE NOT NULL DEFAULT 0,
  fc_desv   DOUBLE AS (IF(fc_n  > 1, SQRT(fc_m2  / (fc_n  - 1)), NULL)) VIRTUAL,
  hrv_desv  DOUBLE AS (IF(hrv_n > 1, SQRT(hrv_m2 / (hrv_n - 1)), NULL)) VIRTUAL,
  fecha_calculo          DATETIME NULL DEFAULT NULL,       -- fecha en que quedó LISTA
  PRIMARY KEY (id),
  UNIQUE KEY uq_linea_base_usuario (usuario_id),
  CONSTRAINT ck_linea_base_dias  CHECK (dias_validos <= 5),
  CONSTRAINT ck_linea_base_lista CHECK (estado = 'EN_CALCULO'
                                        OR (dias_validos = 5 AND fecha_calculo IS NOT NULL)),
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
  desviacion_base  FLOAT        NULL,                      -- desviación respecto a la línea base
  es_parcial       BOOLEAN      NOT NULL DEFAULT FALSE,    -- TRUE si el wearable no entregó HRV
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
  util          BOOLEAN      NULL,                         -- NULL hasta que el usuario la valore
  PRIMARY KEY (id),
  UNIQUE KEY uq_alerta_deteccion (deteccion_id),
  CONSTRAINT fk_alerta_deteccion FOREIGN KEY (deteccion_id)
    REFERENCES deteccion (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Personalización de sugerencias
-- ---------------------------------------------------------------------
CREATE TABLE preferencia_sugerencia (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id  INT UNSIGNED NOT NULL,
  tipo        VARCHAR(50)  NOT NULL,                       -- p. ej. 'pausa activa', 'respiración'
  activa      BOOLEAN      NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  UNIQUE KEY uq_preferencia_usuario_tipo (usuario_id, tipo),
  CONSTRAINT fk_preferencia_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Registro de actividades (HU009): tipo, inicio, fin (o duración) y nota
-- ---------------------------------------------------------------------
CREATE TABLE actividad (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id    INT UNSIGNED NOT NULL,
  tipo          ENUM('REUNION','ENTREGA_CON_PLAZO','TRABAJO_INDIVIDUAL',
                     'PAUSA_DESCANSO','INTERACCION_SOCIAL','OTRA') NOT NULL,
  hora_inicio   DATETIME NOT NULL,
  hora_fin      DATETIME NULL,                             -- NULL si solo se indica la duración
  duracion_min  SMALLINT UNSIGNED NULL,
  nota          VARCHAR(255) NULL,                         -- opcional
  creada_en     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_actividad_usuario_inicio (usuario_id, hora_inicio),
  CONSTRAINT ck_actividad_fin    CHECK (hora_fin IS NULL OR hora_fin > hora_inicio),
  CONSTRAINT ck_actividad_tiempo CHECK (hora_fin IS NOT NULL OR duracion_min IS NOT NULL),
  CONSTRAINT fk_actividad_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Registro de accesos (HU020, HU010, HU024): accesos rechazados por rol y
-- consultas del reporte agregado con sus filtros
-- ---------------------------------------------------------------------
CREATE TABLE registro_acceso (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id  INT UNSIGNED NOT NULL,
  fecha_hora  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  funcion     VARCHAR(150) NOT NULL,                       -- p. ej. 'GET /api/reportes/area'
  resultado   ENUM('PERMITIDO','RECHAZADO') NOT NULL,
  filtros     VARCHAR(255) NULL,                           -- p. ej. 'area=2;periodo=2026-11'
  PRIMARY KEY (id),
  KEY ix_registro_usuario_fecha (usuario_id, fecha_hora),
  KEY ix_registro_resultado (resultado, fecha_hora),
  CONSTRAINT fk_registro_usuario FOREIGN KEY (usuario_id)
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
  periodo         VARCHAR(20)  NOT NULL,                   -- p. ej. '2026-11'
  nivel_promedio  FLOAT        NOT NULL,                   -- 1 = bajo, 2 = moderado, 3 = alto
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
-- (si el cliente no entiende DELIMITER, ejecute cada trigger por separado)
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

INSERT INTO usuario
  (id, nombre_completo, correo, contrasena_hash, rol,
   intentos_fallidos, bloqueado_hasta, hora_chequeo, cargo_id, area_id, creado_en)
VALUES
  (1,  'Laura Gómez Pineda',     'laura.gomez@empresa.com.co',     '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000001', 'SST_TH',    0, NULL, '16:00:00', 3, 4, '2026-09-15 08:00:00'),
  (2,  'Carlos Ramírez Soto',    'carlos.ramirez@empresa.com.co',  '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000002', 'SST_TH',    0, NULL, '16:00:00', 2, 3, '2026-09-15 08:05:00'),
  (3,  'Andrés Martínez Ruiz',   'andres.martinez@empresa.com.co', '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000003', 'TRABAJADOR', 0, NULL, '16:00:00', 2, 5, '2026-10-01 08:00:00'),
  (4,  'Sofía Herrera Lara',     'sofia.herrera@empresa.com.co',   '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000004', 'TRABAJADOR', 0, NULL, '16:00:00', 2, 5, '2026-10-01 08:10:00'),
  (5,  'Julián Torres Medina',   'julian.torres@empresa.com.co',   '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000005', 'TRABAJADOR', 0, NULL, '16:00:00', 3, 5, '2026-10-01 08:15:00'),
  (6,  'Valentina Rojas Cano',   'valentina.rojas@empresa.com.co', '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000006', 'TRABAJADOR', 0, NULL, '15:30:00', 2, 5, '2026-10-01 08:20:00'),
  (7,  'Camilo Duarte Vega',     'camilo.duarte@empresa.com.co',   '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000007', 'TRABAJADOR', 0, NULL, '16:00:00', 1, 5, '2026-10-01 08:25:00'),
  (8,  'Daniela Pardo Ríos',     'daniela.pardo@empresa.com.co',   '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000008', 'TRABAJADOR', 0, NULL, '16:00:00', 2, 5, '2026-10-01 08:30:00'),
  (9,  'Mateo Vargas León',      'mateo.vargas@empresa.com.co',    '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000009', 'TRABAJADOR', 0, NULL, '16:00:00', 1, 2, '2026-10-02 08:00:00'),
  (10, 'Isabela Castro Mora',    'isabela.castro@empresa.com.co',  '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000010', 'TRABAJADOR', 0, NULL, '16:00:00', 2, 2, '2026-10-02 08:05:00'),
  (11, 'Felipe Ortiz Barón',     'felipe.ortiz@empresa.com.co',    '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000011', 'TRABAJADOR', 0, NULL, '17:00:00', 1, 2, '2026-10-02 08:10:00'),
  (12, 'Natalia Suárez Peña',    'natalia.suarez@empresa.com.co',  '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000012', 'TRABAJADOR', 5, '2026-11-06 09:45:00', '16:00:00', 3, 2, '2026-10-02 08:15:00'),
  (13, 'Sebastián Mejía Arias',  'sebastian.mejia@empresa.com.co', '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000013', 'TRABAJADOR', 0, NULL, '16:00:00', 1, 2, '2026-10-02 08:20:00'),
  (14, 'Paula Jiménez Ocampo',   'paula.jimenez@empresa.com.co',   '$2a$10$EjemploHashFicticioNoUsarEnProduccion000000000000014', 'TRABAJADOR', 2, NULL, '16:00:00', 2, 1, '2026-11-02 08:00:00');
-- Natalia (12) aparece bloqueada por 5 intentos fallidos (HU020).

-- ---------------------------------------------------------------------
-- 2. consentimiento: uno vigente por usuario. Sebastián (13) tiene además
--    una versión anterior ya reemplazada (vigente = FALSE).
--    Debe cargarse ANTES que dispositivo_vinculado (lo exige el trigger).
-- ---------------------------------------------------------------------
INSERT INTO consentimiento (usuario_id, version, fecha_aceptacion, vigente) VALUES
  (1,  'v2.0', '2026-09-15 08:10:00', TRUE),
  (2,  'v2.0', '2026-09-15 08:15:00', TRUE),
  (3,  'v2.0', '2026-10-01 08:05:00', TRUE),
  (4,  'v2.0', '2026-10-01 08:12:00', TRUE),
  (5,  'v2.0', '2026-10-01 08:18:00', TRUE),
  (6,  'v2.0', '2026-10-01 08:22:00', TRUE),
  (7,  'v2.0', '2026-10-01 08:27:00', TRUE),
  (8,  'v2.0', '2026-10-01 08:32:00', TRUE),
  (9,  'v2.0', '2026-10-02 08:03:00', TRUE),
  (10, 'v2.0', '2026-10-02 08:08:00', TRUE),
  (11, 'v2.0', '2026-10-02 08:12:00', TRUE),
  (12, 'v2.0', '2026-10-02 08:18:00', TRUE),
  (13, 'v1.0', '2026-10-02 08:22:00', FALSE),
  (13, 'v2.0', '2026-10-20 10:00:00', TRUE),
  (14, 'v2.0', '2026-11-02 08:05:00', TRUE);

-- ---------------------------------------------------------------------
-- 3. dispositivo_vinculado: solo trabajadores (3-14), máximo uno por usuario.
--    Daniela (8) y Felipe (11) tienen un wearable sin HRV.
--    Mateo (9) quedó DESCONECTADO.
-- ---------------------------------------------------------------------
INSERT INTO dispositivo_vinculado
  (usuario_id, plataforma, estado, hrv_disponible, ultima_sincronizacion) VALUES
  (3,  'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 08:15:00'),
  (4,  'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 08:15:00'),
  (5,  'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 08:00:00'),
  (6,  'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 08:15:00'),
  (7,  'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 07:45:00'),
  (8,  'Health Connect', 'VINCULADO',    FALSE, '2026-11-06 08:15:00'),
  (9,  'Health Connect', 'DESCONECTADO', TRUE,  '2026-11-04 18:30:00'),
  (10, 'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 08:00:00'),
  (11, 'Health Connect', 'VINCULADO',    FALSE, '2026-11-06 08:15:00'),
  (12, 'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 07:30:00'),
  (13, 'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 08:15:00'),
  (14, 'Health Connect', 'VINCULADO',    TRUE,  '2026-11-06 08:15:00');

-- ---------------------------------------------------------------------
-- 4. linea_base (Welford). No se insertan fc_desv ni hrv_desv: son columnas
--    generadas. Para 150 ventanas, m2 = desviación² × (n - 1).
--    Usuarios 3-13: LISTA (5 días válidos). Usuario 14: EN_CALCULO (3 días).
--    Daniela (8) y Felipe (11) no tienen HRV: hrv_n = 0 y hrv_base = NULL.
-- ---------------------------------------------------------------------
INSERT INTO linea_base
  (usuario_id, estado, dias_validos, dia_en_curso, ventanas_dia_en_curso,
   fc_n, fc_base, fc_m2, hrv_n, hrv_base, hrv_m2, fecha_calculo) VALUES
  (3,  'LISTA', 5, NULL, 0, 150, 71.8, 4029, 150, 52.4, 18029, '2026-10-08 18:00:00'),
  (4,  'LISTA', 5, NULL, 0, 150, 76.2, 5544, 150, 44.8, 13447, '2026-10-08 18:00:00'),
  (5,  'LISTA', 5, NULL, 0, 150, 68.5, 3433, 150, 58.1, 22910, '2026-10-08 18:00:00'),
  (6,  'LISTA', 5, NULL, 0, 150, 74.9, 4841, 150, 47.3, 15502, '2026-10-08 18:00:00'),
  (7,  'LISTA', 5, NULL, 0, 150, 80.3, 6295, 150, 39.6, 11539, '2026-10-09 18:00:00'),
  (8,  'LISTA', 5, NULL, 0, 150, 72.1, 3725,   0, NULL,     0, '2026-10-09 18:00:00'),
  (9,  'LISTA', 5, NULL, 0, 150, 69.7, 4345, 150, 55.0, 20049, '2026-10-09 18:00:00'),
  (10, 'LISTA', 5, NULL, 0, 150, 77.4, 5364, 150, 46.2, 14603, '2026-10-09 18:00:00'),
  (11, 'LISTA', 5, NULL, 0, 150, 73.6, 4507,   0, NULL,     0, '2026-10-09 18:00:00'),
  (12, 'LISTA', 5, NULL, 0, 150, 70.9, 3875, 150, 50.7, 17379, '2026-10-09 18:00:00'),
  (13, 'LISTA', 5, NULL, 0, 150, 75.5, 5728, 150, 43.9, 12339, '2026-10-25 18:00:00'),
  (14, 'EN_CALCULO', 3, '2026-11-06', 9, 90, 74.0, 2692, 90, 49.5, 8900, NULL);

-- ---------------------------------------------------------------------
-- 5. ventana_senal: ventanas de 15 min del 06/11/2026 (retención de 24 h).
--    Andrés (3) con HRV; Daniela (8) sin HRV (hrv_promedio NULL).
-- ---------------------------------------------------------------------
INSERT INTO ventana_senal
  (usuario_id, inicio, fin, fc_promedio, hrv_promedio, num_lecturas, recibida_en) VALUES
  (3, '2026-11-06 07:00:00', '2026-11-06 07:15:00', 70.4, 54.1, 15, '2026-11-06 07:16:00'),
  (3, '2026-11-06 07:15:00', '2026-11-06 07:30:00', 72.9, 51.8, 15, '2026-11-06 07:31:00'),
  (3, '2026-11-06 07:30:00', '2026-11-06 07:45:00', 75.3, 47.2, 14, '2026-11-06 07:46:00'),
  (3, '2026-11-06 07:45:00', '2026-11-06 08:00:00', 78.0, 44.6, 15, '2026-11-06 08:01:00'),
  (8, '2026-11-06 07:00:00', '2026-11-06 07:15:00', 71.2, NULL, 12, '2026-11-06 07:16:00'),
  (8, '2026-11-06 07:15:00', '2026-11-06 07:30:00', 73.5, NULL, 13, '2026-11-06 07:31:00'),
  (4, '2026-11-06 07:00:00', '2026-11-06 07:15:00', 76.8, 43.9, 15, '2026-11-06 07:16:00'),
  (4, '2026-11-06 07:15:00', '2026-11-06 07:30:00', 79.1, 41.2, 15, '2026-11-06 07:31:00');

-- ---------------------------------------------------------------------
-- 6. deteccion (14). Los ids son explícitos porque alerta los referencia.
--    Daniela (8) y Felipe (11): es_parcial = TRUE (sin HRV).
--    Con estos datos, Tecnología (6 trabajadores) y Operaciones (5)
--    cumplen el mínimo de 5 en la vista de noviembre.
-- ---------------------------------------------------------------------
INSERT INTO deteccion (id, usuario_id, fecha_hora, nivel, desviacion_base, es_parcial) VALUES
  (1,  3,  '2026-11-02 10:30:00', 'BAJO',     -0.35, FALSE),
  (2,  3,  '2026-11-03 15:45:00', 'ALTO',      2.60, FALSE),
  (3,  4,  '2026-11-02 11:00:00', 'MODERADO',  1.10, FALSE),
  (4,  5,  '2026-11-03 09:15:00', 'BAJO',      0.20, FALSE),
  (5,  6,  '2026-11-04 14:30:00', 'ALTO',      2.85, FALSE),
  (6,  7,  '2026-11-04 16:00:00', 'MODERADO',  1.45, FALSE),
  (7,  8,  '2026-11-05 10:00:00', 'MODERADO',  0.95, TRUE),
  (8,  9,  '2026-11-02 13:20:00', 'BAJO',      0.10, FALSE),
  (9,  10, '2026-11-03 11:40:00', 'MODERADO',  1.30, FALSE),
  (10, 11, '2026-11-04 09:50:00', 'ALTO',      2.40, TRUE),
  (11, 12, '2026-11-04 15:10:00', 'BAJO',     -0.10, FALSE),
  (12, 13, '2026-11-05 12:25:00', 'MODERADO',  1.20, FALSE),
  (13, 4,  '2026-11-05 16:20:00', 'ALTO',      3.05, FALSE),
  (14, 5,  '2026-11-05 14:05:00', 'BAJO',      0.05, FALSE);

-- ---------------------------------------------------------------------
-- 7. alerta: solo para detecciones ALTO (2, 5, 10, 13), como exige el
--    trigger. `util` solo tiene valor cuando el estado es VALORADA.
-- ---------------------------------------------------------------------
INSERT INTO alerta (deteccion_id, creada_en, sugerencia, estado, util) VALUES
  (2,  '2026-11-03 15:46:00', 'Realice una pausa activa de 5 minutos y estire cuello y hombros.',      'VALORADA',   TRUE),
  (5,  '2026-11-04 14:31:00', 'Pruebe 3 minutos de respiración diafragmática lenta.',                   'POSPUESTA',  NULL),
  (10, '2026-11-04 09:51:00', 'Camine unos minutos lejos de su puesto y tome agua.',                    'DESCARTADA', NULL),
  (13, '2026-11-05 16:21:00', 'Haga una pausa de respiración y revise sus prioridades de la tarde.',    'ENVIADA',    NULL);

-- ---------------------------------------------------------------------
-- 8. cuestionario
--    DIARIO : puntaje 0-10, energía 1-5 y tensión obligatorias.
--    MENSUAL: puntaje PSS-10 0-40, energía y tensión en NULL.
-- ---------------------------------------------------------------------
INSERT INTO cuestionario (usuario_id, tipo, puntaje, energia, tension, fecha_hora) VALUES
  (3,  'DIARIO',   4, 4, FALSE, '2026-11-02 16:00:00'),
  (3,  'DIARIO',   8, 2, TRUE,  '2026-11-03 16:00:00'),
  (4,  'DIARIO',   6, 3, TRUE,  '2026-11-02 16:00:00'),
  (5,  'DIARIO',   2, 5, FALSE, '2026-11-03 16:05:00'),
  (6,  'DIARIO',   9, 1, TRUE,  '2026-11-04 15:30:00'),
  (10, 'DIARIO',   5, 3, FALSE, '2026-11-03 16:00:00'),
  (11, 'DIARIO',   7, 2, TRUE,  '2026-11-04 17:00:00'),
  (3,  'MENSUAL', 18, NULL, NULL, '2026-10-30 09:00:00'),
  (6,  'MENSUAL', 27, NULL, NULL, '2026-10-30 09:10:00'),
  (9,  'MENSUAL', 12, NULL, NULL, '2026-10-30 09:20:00');

-- ---------------------------------------------------------------------
-- 9. preferencia_sugerencia (única por usuario y tipo)
-- ---------------------------------------------------------------------
INSERT INTO preferencia_sugerencia (usuario_id, tipo, activa) VALUES
  (3,  'pausa activa', TRUE),
  (3,  'respiración',  TRUE),
  (3,  'caminata',     FALSE),
  (4,  'respiración',  TRUE),
  (6,  'pausa activa', TRUE),
  (6,  'estiramiento', TRUE),
  (10, 'caminata',     TRUE);

-- ---------------------------------------------------------------------
-- 10. actividad: algunas con hora de fin y otras solo con duración.
--     Debe tener hora_fin o duracion_min, y si hay fin, debe ser mayor
--     que el inicio.
-- ---------------------------------------------------------------------
INSERT INTO actividad
  (usuario_id, tipo, hora_inicio, hora_fin, duracion_min, nota, creada_en) VALUES
  (3,  'REUNION',            '2026-11-03 09:00:00', '2026-11-03 10:00:00', NULL, 'Reunión de seguimiento del sprint', '2026-11-03 10:05:00'),
  (3,  'ENTREGA_CON_PLAZO',  '2026-11-03 13:00:00', NULL,                  120,  'Entrega de informe al cliente',    '2026-11-03 15:10:00'),
  (4,  'TRABAJO_INDIVIDUAL', '2026-11-02 08:30:00', '2026-11-02 11:30:00', NULL, NULL,                               '2026-11-02 11:35:00'),
  (5,  'PAUSA_DESCANSO',     '2026-11-03 12:00:00', NULL,                  30,   'Almuerzo',                         '2026-11-03 12:35:00'),
  (6,  'INTERACCION_SOCIAL', '2026-11-04 15:00:00', '2026-11-04 15:20:00', NULL, 'Café con el equipo',                '2026-11-04 15:25:00'),
  (9,  'OTRA',               '2026-11-02 14:00:00', NULL,                  45,   'Capacitación interna',             '2026-11-02 14:50:00'),
  (10, 'REUNION',            '2026-11-03 10:30:00', '2026-11-03 11:15:00', NULL, 'Revisión de turnos',               '2026-11-03 11:20:00');

-- ---------------------------------------------------------------------
-- 11. registro_acceso: consultas permitidas de SST/TH y accesos rechazados
-- ---------------------------------------------------------------------
INSERT INTO registro_acceso (usuario_id, fecha_hora, funcion, resultado, filtros) VALUES
  (1,  '2026-11-05 09:10:00', 'GET /api/reportes/area', 'PERMITIDO', 'area=5;periodo=2026-10'),
  (2,  '2026-11-05 09:40:00', 'GET /api/reportes/area', 'PERMITIDO', 'area=2;periodo=2026-10'),
  (3,  '2026-11-05 10:02:00', 'GET /api/reportes/area', 'RECHAZADO', NULL),
  (14, '2026-11-05 11:15:00', 'GET /api/reportes/area', 'RECHAZADO', NULL),
  (12, '2026-11-06 09:30:00', 'POST /api/auth/login',   'RECHAZADO', NULL);

-- ---------------------------------------------------------------------
-- 12. reporte_agregado: periodo cerrado (octubre). Cada área cumple el
--     mínimo de 5 participantes (CHECK de anonimato).
-- ---------------------------------------------------------------------
INSERT INTO reporte_agregado (area_id, periodo, nivel_promedio, participantes) VALUES
  (5, '2026-10', 1.83, 6),
  (2, '2026-10', 1.60, 5);

-- ---------------------------------------------------------------------
-- Verificación: la vista debe devolver noviembre para Tecnología (6) y
-- Operaciones (5), con promedios de 2.00 y 1.80.
-- ---------------------------------------------------------------------
SELECT * FROM v_nivel_por_area_mes;