-- 00_crear_usuario.sql
-- Ejecutar en SQL Developer conectado a FREEPDB1 como SYS con rol SYSDBA.
-- Usuario propietario del esquema: OSIN.

ALTER SESSION SET "_oracle_script" = TRUE;

CREATE USER osin IDENTIFIED BY osin;

GRANT ALL PRIVILEGES TO osin;

COMMIT;