-- HU001: verificación del correo con un código de 6 dígitos.
-- Ejecutar una sola vez sobre una base de datos ya creada con proyecto.sql.
-- (Una base creada desde cero con el proyecto.sql actual ya trae estas columnas.)
USE gestion_estres;

ALTER TABLE usuario
  ADD COLUMN correo_verificado        BOOLEAN NOT NULL DEFAULT FALSE AFTER correo,
  ADD COLUMN codigo_verificacion_hash CHAR(64) NULL AFTER correo_verificado,
  ADD COLUMN codigo_expira            DATETIME NULL AFTER codigo_verificacion_hash,
  ADD COLUMN intentos_codigo          TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER codigo_expira;

-- Las cuentas que ya existían quedan verificadas para no bloquearlas.
UPDATE usuario SET correo_verificado = TRUE WHERE id > 0;   -- WHERE con la llave: lo exige el modo seguro de Workbench
