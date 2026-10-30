package com.lacomer.osinapi.entities;

import com.lacomer.osinapi.converters.BooleanToNumberConverter;
import com.lacomer.osinapi.exceptions.ConflictoException;
import com.lacomer.osinapi.utils.StringCustomUtils;
import com.lacomer.osinapi.utils.ValoresNumericosUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Table(name = "PRODUCTOS")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "NOMBRE", nullable = false)
    private String nombre;

    @Column(name = "DESCRIPCION")
    private String descripcion;

    @Column(name = "PRECIO_UNITARIO", nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "EXISTENCIAS", nullable = false)
    private Integer existencias;

    @Builder.Default
    @Convert(converter = BooleanToNumberConverter.class)
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;

    // Bloqueo optimista: si dos pedidos modifican la existencia al mismo tiempo,
    // el segundo falla al guardar con ObjectOptimisticLockingFailureException (409) y se hace rollback
    @Version
    @Column(name = "VERSION_LOCK", nullable = false)
    private Long version;

    private static String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }

    // Oracle guarda '' como NULL, por eso una descripcion vacia se normaliza a null
    private static String limpiarOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static void validarDatos(
            String nombre,
            String descripcion,
            BigDecimal precioUnitario,
            Integer existencias
    ) {
        StringCustomUtils.validarTamano(
                nombre, 1, 120,
                "El nombre del producto debe contener entre 1 y 120 caracteres"
        );
        if (descripcion != null)
            StringCustomUtils.validarTamano(
                    descripcion, 1, 500,
                    "La descripcion debe contener como maximo 500 caracteres"
            );
        ValoresNumericosUtils.validarPositivo(
                precioUnitario,
                "El precio unitario es requerido y debe ser mayor a 0"
        );
        ValoresNumericosUtils.validarRango(
                existencias, 0, Integer.MAX_VALUE,
                "La existencia del producto es requerida y no puede ser negativa"
        );
    }

    public void desactivar() {
        if (!this.activo)
            throw new ConflictoException("El producto ya se encuentra desactivado");
        this.activo = false;
    }

    public void activar() {
        if (this.activo)
            throw new ConflictoException("El producto ya se encuentra activado");
        this.activo = true;
    }

    public void aumentarExistencias(Integer cantidad) {
        ValoresNumericosUtils.validarPositivo(
                cantidad,
                "La cantidad a agregar debe ser positiva"
        );
        this.existencias += cantidad;
    }

    public void validarExistenciasSuficientes(Integer cantidad) {
        ValoresNumericosUtils.validarPositivo(
                cantidad,
                "La cantidad solicitada debe ser positiva"
        );
        if (cantidad > this.existencias) {
            throw new ConflictoException(
                    "Stock insuficiente para el producto '" + this.nombre + "': disponible "
                            + this.existencias + ", solicitado " + cantidad
            );
        }
    }

    public void decrementarExistencias(Integer cantidad) {
        validarExistenciasSuficientes(cantidad);
        this.existencias -= cantidad;
    }

    public void actualizar(
            String nombre,
            String descripcion,
            BigDecimal precioUnitario,
            Integer existencias
    ) {
        String nombreLimpio = limpiar(nombre);
        String descripcionLimpia = limpiarOpcional(descripcion);
        validarDatos(nombreLimpio, descripcionLimpia, precioUnitario, existencias);

        this.nombre = nombreLimpio;
        this.descripcion = descripcionLimpia;
        this.precioUnitario = precioUnitario;
        this.existencias = existencias;
    }

    public static Producto crear(
            String nombre,
            String descripcion,
            BigDecimal precioUnitario,
            Integer existencias
    ) {
        String nombreLimpio = limpiar(nombre);
        String descripcionLimpia = limpiarOpcional(descripcion);
        validarDatos(nombreLimpio, descripcionLimpia, precioUnitario, existencias);
        return Producto.builder()
                .nombre(nombreLimpio)
                .descripcion(descripcionLimpia)
                .precioUnitario(precioUnitario)
                .existencias(existencias)
                .build();
    }
}
