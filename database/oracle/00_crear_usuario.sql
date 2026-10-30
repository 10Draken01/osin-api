-- 00_crear_usuario.sql
-- Ejecutar en SQL Developer conectado a FREEPDB1 como SYSTEM (o SYS con rol SYSDBA).
-- Usuario propietario del esquema: OSIN. La contrasena es un valor de ejemplo para desarrollo local.

alter session set "_oracle_script"=true;
create user osin identified by osin;
GRANT ALL PRIVILEGES TO osin;
commit;