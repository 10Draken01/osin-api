-- Solo para el perfil de pruebas: Hibernate crea las tablas (create-drop) y aqui se crea la secuencia del folio
-- (en Oracle la crea database/oracle/01_schema.sql).
CREATE SEQUENCE IF NOT EXISTS SEQ_PEDIDOS_FOLIO START WITH 1 INCREMENT BY 1;
