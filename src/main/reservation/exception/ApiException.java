package reservation.exception;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erreur metier traduite en reponse HTTP par le {@link GlobalExceptionHandler}.
 *
 * <p>Le {@link ApiErrorCode} porte le statut HTTP, tandis que {@code details}
 * transporte les informations contextuelles exposees dans la reponse, par exemple
 * l'identifiant de la ressource introuvable.</p>
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final ApiErrorCode code;
    private final Map<String, Object> details;

    protected ApiException(ApiErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : new LinkedHashMap<>(details);
    }

    protected ApiException(ApiErrorCode code, String message) {
        this(code, message, Map.of());
    }
}
