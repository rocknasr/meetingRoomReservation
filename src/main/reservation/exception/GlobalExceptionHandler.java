package reservation.exception;

import reservation.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduit toutes les erreurs de l'application en reponses {@code ApiErrorResponse},
 * pour que le format des erreurs soit identique sur l'ensemble de l'API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_MESSAGE = "La requete contient des donnees invalides";

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            ValidationException exception, HttpServletRequest request) {
        return build(exception.getCode(), exception.getMessage(), request,
                exception.getDetails(), exception.getFieldErrors());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException exception, HttpServletRequest request) {
        return build(exception.getCode(), exception.getMessage(), request,
                exception.getDetails(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBody(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return build(ApiErrorCode.VALIDATION_ERROR, VALIDATION_MESSAGE, request, Map.of(), fieldErrors);
    }

    // Parametres de requete invalides, par exemple capacity=0 sur /api/rooms/available.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidParameters(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String name = result.getMethodParameter().getParameterName();
            if (name == null) {
                name = "request";
            }
            if (!result.getResolvableErrors().isEmpty()) {
                fieldErrors.putIfAbsent(name, result.getResolvableErrors().get(0).getDefaultMessage());
            }
        }
        if (fieldErrors.isEmpty()) {
            fieldErrors.put("request", VALIDATION_MESSAGE);
        }
        return build(ApiErrorCode.VALIDATION_ERROR, VALIDATION_MESSAGE, request, Map.of(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            fieldErrors.putIfAbsent(lastPathNode(violation), violation.getMessage());
        }
        return build(ApiErrorCode.VALIDATION_ERROR, VALIDATION_MESSAGE, request, Map.of(), fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return build(ApiErrorCode.VALIDATION_ERROR, VALIDATION_MESSAGE, request, Map.of(),
                Map.of(exception.getParameterName(), "est obligatoire"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return build(ApiErrorCode.VALIDATION_ERROR, VALIDATION_MESSAGE, request, Map.of(),
                Map.of(exception.getName(), "a un format invalide"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return build(ApiErrorCode.VALIDATION_ERROR, VALIDATION_MESSAGE, request, Map.of(),
                Map.of("body", "est absent ou mal forme"));
    }

    // Filet de securite si deux requetes simultanees passent le controle d'unicite du service.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        return build(ApiErrorCode.RESOURCE_ALREADY_EXISTS,
                "Une valeur soumise a une contrainte d'unicite est deja utilisee",
                request, Map.of(), Map.of());
    }

    private ResponseEntity<ApiErrorResponse> build(
            ApiErrorCode code,
            String message,
            HttpServletRequest request,
            Map<String, Object> details,
            Map<String, String> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                code.name(),
                message,
                Instant.now(),
                request.getRequestURI(),
                details,
                fieldErrors);
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    // Transforme "findAvailableRooms.capacity" en "capacity".
    private String lastPathNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot < 0 ? path : path.substring(lastDot + 1);
    }
}
