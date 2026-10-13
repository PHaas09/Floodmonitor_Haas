package com.example.floodmonitor.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Zentrale Fehlerbehandlung: macht aus jeder Exception ein ApiError.
 * Spring schreibt es als JSON oder XML, je nach Accept-Header.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Pfad existiert nicht", request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /** z.B. limit=abc oder isActive=vielleicht */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> wrongType(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Ungültiger Wert für Parameter '" + e.getName() + "': " + e.getValue(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> wrongMethod(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Methode " + e.getMethod() + " wird nicht unterstützt", request);
    }

    /** Accept-Header verlangt ein Format, das wir nicht liefern (z.B. text/csv) -> 406, Antwort als JSON. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> notAcceptable(HttpMediaTypeNotAcceptableException e, HttpServletRequest request) {
        ApiError body = ApiError.of(HttpStatus.NOT_ACCEPTABLE,
                "Format nicht unterstützt. Erlaubt: application/json, application/xml", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    /** Alles Unerwartete: Details nur ins Log, nie zum Client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception e, HttpServletRequest request) {
        log.error("Fehler bei {}", request.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Serverfehler", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI()));
    }
}
