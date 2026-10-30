-- 00_crear_usuario.sql
-- Ejecutar en SQL Developer conectado a FREEPDB1 como SYSTEM (o SYS con rol SYSDBA).
-- Usuario propietario del esquema: OSIN. La contrasena es un valor de ejemplo para desarrollo local.

CREATE USER osin IDENTIFIED BY osin
    DEFAULT TABLESPACE users
    QUOTA UNLIMITED ON users;

-- Solo los permisos que necesita el proyecto
GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE TO osin;
