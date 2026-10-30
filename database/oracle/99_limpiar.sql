-- 99_limpiar.sql
-- Ejecutar conectado como OSIN en FREEPDB1.
-- Elimina los objetos del esquema, pero NO elimina el usuario OSIN.

DROP VIEW vw_pedido_resumen;
DROP TABLE pedido_detalle CASCADE CONSTRAINTS PURGE;
DROP TABLE pedido CASCADE CONSTRAINTS PURGE;
DROP TABLE producto CASCADE CONSTRAINTS PURGE;
DROP SEQUENCE seq_pedido_folio;
