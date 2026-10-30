package com.lacomer.osinapi.utils;

import com.lacomer.osinapi.exceptions.DatoInvalidoException;

public class StringCustomUtils {

    public static void validarTamano(String texto, int min, int max, String mensajeError){
        validarNoVacioNoNull(texto, mensajeError);
        if(texto.trim().length() < min || texto.trim().length() > max)
            throw new DatoInvalidoException(mensajeError);
    }

    public static void validarNoVacioNoNull(String texto, String mensajeError){
        if(texto == null || texto.isBlank())
            throw new DatoInvalidoException(mensajeError) ;
    }

    public static String normalizarTexto(String texto){
        return texto
                .replace("á", "a")
                .replace("à", "a")
                .replace("ä", "a")
                .replace("â", "a")
                .replace("ã", "a")
                .replace("å", "a")

                .replace("é", "e")
                .replace("è", "e")
                .replace("ë", "e")
                .replace("ê", "e")

                .replace("í", "i")
                .replace("ì", "i")
                .replace("ï", "i")
                .replace("î", "i")

                .replace("ó", "o")
                .replace("ò", "o")
                .replace("ö", "o")
                .replace("ô", "o")
                .replace("õ", "o")
                .replace("ø", "o")

                .replace("ú", "u")
                .replace("ù", "u")
                .replace("ü", "u")
                .replace("û", "u")

                .replace("ñ", "n")
                .replace("ç", "c")
                .replace("ý", "y")
                .replace("ÿ", "y")

                .trim();
    }

    public static void validarTextoNumerico(String texto, String mensajeError){
        validarNoVacioNoNull(texto, mensajeError);
        if(!texto.trim().matches("\\d+"))
            throw new DatoInvalidoException(mensajeError);
    }
}
