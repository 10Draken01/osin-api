package com.lacomer.osinapi.utils;

import com.lacomer.osinapi.exceptions.DatoInvalidoException;

import java.math.BigDecimal;

public final class ValoresNumericosUtils {

    private ValoresNumericosUtils() {
    }

    public static <N extends Number> void validarNumeroRequerido(
            N numero,
            String mensajeError
    ) {
        if (numero == null)
            throw new DatoInvalidoException(mensajeError);
    }

    public static <N extends Number> void validarPositivo(
            N numero,
            String mensajeError
    ) {
        validarNumeroRequerido(numero, mensajeError);

        BigDecimal valor = convertir(numero);

        if (valor.compareTo(BigDecimal.ZERO) <= 0)
            throw new DatoInvalidoException(mensajeError);
    }

    public static <N extends Number> void validarRango(
            N numero,
            N minimo,
            N maximo,
            String mensajeError
    ) {
        validarNumeroRequerido(numero, mensajeError);
        validarNumeroRequerido(minimo, mensajeError);
        validarNumeroRequerido(maximo, mensajeError);

        BigDecimal valor = convertir(numero);
        BigDecimal min = convertir(minimo);
        BigDecimal max = convertir(maximo);

        if (valor.compareTo(min) < 0 || valor.compareTo(max) > 0)
            throw new DatoInvalidoException(mensajeError);
    }

    private static BigDecimal convertir(Number numero) {
        return new BigDecimal(numero.toString());
    }
}
