package com.lacomer.osinapi.utils;


import com.lacomer.osinapi.exceptions.DatoInvalidoException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {
    private static final DateTimeFormatter FORMATO_FECHA_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_YYYY_MM_DD = DateTimeFormatter.ofPattern("uuuu-MM-dd");

    public static void validarFormatoHora(String hora, String mensaje) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("HH:mm");

        try {
            LocalTime.parse(hora, formatter);
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException(mensaje);
        }
    }

    public static void validarFormatoPeriodo(String periodo, String mensaje) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("uuuu-MM");

        try {
            YearMonth.parse(periodo, formatter);
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException(mensaje);
        }
    }

    public static void validarHoraInicioFin(String horaInicio, String horaFin, String mensaje) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime inicio = LocalTime.parse(horaInicio, formatter);
        LocalTime fin = LocalTime.parse(horaFin, formatter);

        if (!inicio.isBefore(fin)) {
            throw new DatoInvalidoException(mensaje);
        }
    }

    public static Integer obtenerDiasEnRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechaRequerida(fechaInicio, "La fecha de inicio es requerida");
        validarFechaRequerida(fechaFin, "La fecha de fin es requerida");
        return (int) (fechaFin.toEpochDay() - fechaInicio.toEpochDay());
    }

    public static void validarFechaRequerida(LocalDate fecha, String mensaje) {
        if (fecha == null) {
            throw new DatoInvalidoException(mensaje);
        }
    }

    public static String localDateAStringFormato_DD_MM_YYYY(LocalDate fecha) {
        return fecha == null ? null : fecha.format(FORMATO_FECHA_DD_MM_YYYY);
    }

    public static String localDateAStringFormato_YYYY_MM_DD(LocalDate fecha) {
        return fecha == null ? null : fecha.format(FORMATO_FECHA_YYYY_MM_DD);
    }

    public static void validarFechaMayorActual(LocalDate fecha, String mensaje) {
        // Valida que la fecha no sea nula y que sea mayor a la fecha actual
        if (fecha == null || !fecha.isAfter(LocalDate.now())) {
            throw new DatoInvalidoException(mensaje);
        }
    }

    public static void validarFechaInicioMenorFin(LocalDate fechaInicio, LocalDate fechaFin, String mensaje) {
        // Valida que fechaInicio y fechaFin no sean nulos y que fechaInicio sea menor a fechaFin
        if (fechaInicio == null || fechaFin == null || !fechaInicio.isBefore(fechaFin)) {
            throw new DatoInvalidoException(mensaje);
        }
    }
}
