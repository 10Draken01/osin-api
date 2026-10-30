-- 02_data.sql
-- Ejecutar conectado como OSIN en FREEPDB1.

INSERT INTO producto (nombre, descripcion, precio_unitario, existencia, activo)
VALUES ('Arroz 1 kg', 'Arroz blanco de grano largo', 24.90, 100, 1);

INSERT INTO producto (nombre, descripcion, precio_unitario, existencia, activo)
VALUES ('Frijol 1 kg', 'Frijol negro', 32.00, 80, 1);

INSERT INTO producto (nombre, descripcion, precio_unitario, existencia, activo)
VALUES ('Aceite 1 L', 'Aceite vegetal', 45.00, 60, 1);

INSERT INTO producto (nombre, descripcion, precio_unitario, existencia, activo)
VALUES ('Azucar 1 kg', 'Azucar estandar', 28.00, 90, 1);

INSERT INTO producto (nombre, descripcion, precio_unitario, existencia, activo)
VALUES ('Leche 1 L', 'Leche entera', 27.00, 120, 1);

INSERT INTO producto (nombre, descripcion, precio_unitario, existencia, activo)
VALUES ('Cafe 250 g', 'Cafe molido', 68.00, 40, 1);

INSERT INTO producto (nombre, descripcion, precio_unitario, existencia, activo)
VALUES ('Pasta 200 g', 'Pasta de trigo', 15.00, 150, 1);

INSERT INTO producto (nombre, descripcion, precio_unitario, existencia, activo)
VALUES ('Galletas', 'Galletas de vainilla', 19.90, 70, 1);

INSERT INTO pedido (folio, estado)
VALUES ('PED-000001', 'BORRADOR');

INSERT INTO pedido (folio, estado)
VALUES ('PED-000002', 'CONFIRMADO');

INSERT INTO pedido (
    folio,
    estado,
    fecha_cancelacion
)
VALUES (
    'PED-000003',
    'CANCELADO',
    SYSTIMESTAMP
);

-- PED-000001 = 87.90
INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 1, 24.90
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000001' AND pr.nombre = 'Arroz 1 kg';

INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 1, 32.00
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000001' AND pr.nombre = 'Frijol 1 kg';

INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 1, 31.00
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000001' AND pr.nombre = 'Aceite 1 L';

-- PED-000002 = 242.00
INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 2, 45.00
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000002' AND pr.nombre = 'Aceite 1 L';

INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 1, 68.00
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000002' AND pr.nombre = 'Cafe 250 g';

INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 2, 42.00
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000002' AND pr.nombre = 'Frijol 1 kg';

-- PED-000003 = 140.90
INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 1, 68.00
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000003' AND pr.nombre = 'Cafe 250 g';

INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 1, 45.00
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000003' AND pr.nombre = 'Aceite 1 L';

INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario)
SELECT p.id, pr.id, 1, 27.90
FROM pedido p CROSS JOIN producto pr
WHERE p.folio = 'PED-000003' AND pr.nombre = 'Leche 1 L';

COMMIT;
