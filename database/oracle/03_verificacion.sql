-- 03_verificacion.sql
-- Ejecutar conectado como OSIN en FREEPDB1.

PROMPT === PRODUCTOS ===
SELECT id, nombre, precio_unitario, existencia, activo
FROM producto
ORDER BY id;

PROMPT === RESUMEN DE PEDIDOS ===
SELECT folio, estado, total, numero_partidas
FROM vw_pedido_resumen
ORDER BY id;

PROMPT === DETALLE DE PEDIDOS ===
SELECT
    p.folio,
    pr.nombre AS producto,
    d.cantidad,
    d.precio_unitario,
    d.subtotal
FROM pedido_detalle d
JOIN pedido p ON p.id = d.pedido_id
JOIN producto pr ON pr.id = d.producto_id
ORDER BY p.id, d.id;

PROMPT === OBJETOS DEL ESQUEMA ===
SELECT object_type, object_name, status
FROM user_objects
WHERE object_name IN (
    'PRODUCTO',
    'PEDIDO',
    'PEDIDO_DETALLE',
    'VW_PEDIDO_RESUMEN',
    'SEQ_PEDIDO_FOLIO'
)
ORDER BY object_type, object_name;

PROMPT === RESTRICCIONES ===
SELECT table_name, constraint_name, constraint_type, status
FROM user_constraints
WHERE table_name IN ('PRODUCTO', 'PEDIDO', 'PEDIDO_DETALLE')
ORDER BY table_name, constraint_name;