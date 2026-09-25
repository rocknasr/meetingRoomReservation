package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/** Format commun a toutes les erreurs de l'API. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private String code;
    private String message;
    private Instant timestamp;
    private String path;
    private Map<String, Object> details;
    private Map<String, String> fieldErrors;
}
