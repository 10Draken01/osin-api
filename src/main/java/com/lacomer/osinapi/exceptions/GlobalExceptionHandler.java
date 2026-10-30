package com.lacomer.osinapi.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---------- Nuestras excepciones ----------

    // 404: el recurso no existe
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNoEncontrado(RecursoNoEncontradoException e) {
        log.warn("No encontrado: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 400: el cliente mandó un dato inválido
    @ExceptionHandler(DatoInvalidoException.class)
    public ProblemDetail handleDatoInvalido(DatoInvalidoException e) {
        log.warn("Dato inválido: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 409: regla de negocio violada (duplicado, etc.)
    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail handleConflicto(ConflictoException e) {
        log.warn("Conflicto: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    // 409: la base de datos rechazó el dato (unique, foreign key...)
    @ExceptionHandler(EntidadRelacionadaException.class)
    public ProblemDetail handleEntidadRelacionada(EntidadRelacionadaException e) {
        log.warn("Entidad relacionada: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    // 409: la base de datos rechazo el dato (nombre duplicado, folio duplicado, FK...)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegridadDatos(DataIntegrityViolationException e) {
        log.warn("Violacion de integridad de datos: {}", e.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operacion viola una restriccion de datos (por ejemplo, un registro duplicado).");
    }

    // 409: otro usuario modifico el mismo registro al mismo tiempo (@Version)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleBloqueoOptimista(ObjectOptimisticLockingFailureException e) {
        log.warn("Conflicto de concurrencia: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "El registro fue modificado por otro usuario. Recarga la informacion e intenta de nuevo.");
    }

    // ---------- Validaciones (sobrescriben a la clase base) ----------

    // 400: falló @Valid en el @RequestBody
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request
    ) {

        List<String> errores = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos.");
        pd.setProperty("errores", errores);
        return handleExceptionInternal(e, pd, headers, status, request);
    }

    // 400: falló @Positive, @Min... en un @PathVariable o @RequestParam
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException e, HttpHeaders headers,
            HttpStatusCode status, WebRequest request
    ) {

        List<String> errores = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o más parámetros no son válidos.");
        pd.setProperty("errores", errores);
        return handleExceptionInternal(e, pd, headers, status, request);
    }

    // 400: un parametro no tiene el tipo esperado (por ejemplo estado=INVENTADO o pagina=abc)
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException e, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El valor '" + e.getValue() + "' no es valido para el parametro " + e.getPropertyName());
        return handleExceptionInternal(e, pd, headers, status, request);
    }

    // 400: el cuerpo no es un JSON valido o un campo tiene un tipo incorrecto
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El cuerpo de la peticion no es un JSON valido.");
        return handleExceptionInternal(e, pd, headers, status, request);
    }

    // 400: falló @Positive, @Min... en un @PathVariable/@RequestParam (AOP lanza ConstraintViolationException)
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Parámetro inválido: {}", e.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o más parámetros no son válidos.");
        pd.setProperty("errores", e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList());
        return pd;
    }

    // 503: servicio externo no disponible
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ProblemDetail handleServicioNoDisponible(ServicioNoDisponibleException e) {
        log.error("Servicio externo no disponible", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio externo no disponible. Contacte al administrador.");
    }

    // ---------- Red de seguridad ----------

    // 500: cualquier error que no previmos
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception e) {
        log.error("Error interno", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor. Contacte al administrador.");
    }
}
