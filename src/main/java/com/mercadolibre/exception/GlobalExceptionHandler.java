package com.mercadolibre.exception;

import com.mercadolibre.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para centralizar el manejo de errores
 * en toda la aplicación.
 *
 * Proporciona respuestas consistentes y logging automático de errores.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de validación de argumentos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("Error de validación en request: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.badRequest().body(
            new ApiResponse<>(false, null, null,
                "Errores de validación: " + errors.toString())
        );
    }

    /**
     * Maneja excepciones de URL inválida
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Argumento ilegal: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(
            new ApiResponse<>(false, null, null,
                "Solicitud inválida: " + ex.getMessage())
        );
    }

    /**
     * Maneja excepciones de IO (conexión a sitios web)
     */
    @ExceptionHandler(java.io.IOException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<?> handleIOException(java.io.IOException ex) {
        logger.error("Error de conexión: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            new ApiResponse<>(false, null, null,
                "No se pudo conectar al sitio. Por favor, intente más tarde.")
        );
    }

    /**
     * Maneja excepciones de timeout
     */
    @ExceptionHandler(java.net.SocketTimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public ResponseEntity<?> handleTimeoutException(java.net.SocketTimeoutException ex) {
        logger.error("Timeout en conexión: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(
            new ApiResponse<>(false, null, null,
                "Timeout: La solicitud tardó demasiado. Por favor, intente nuevamente.")
        );
    }

    /**
     * Maneja excepciones de operación no soportada
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ResponseEntity<?> handleUnsupportedOperationException(UnsupportedOperationException ex) {
        logger.warn("Operación no soportada: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
            new ApiResponse<>(false, null, null,
                "Operación no soportada: " + ex.getMessage())
        );
    }

    /**
     * Maneja excepciones genéricas
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        logger.error("Error no controlado: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiResponse<>(false, null, null,
                "Error interno del servidor. Por favor, contacte al administrador.")
        );
    }

    /**
     * Maneja excepciones de null pointer
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<?> handleNullPointerException(NullPointerException ex) {
        logger.error("NullPointerException: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiResponse<>(false, null, null,
                "Error procesando la solicitud. El dato requerido no fue encontrado.")
        );
    }
}

