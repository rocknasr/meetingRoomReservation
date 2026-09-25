package reservation.exception;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Champ invalide detecte par une regle metier plutot que par la validation de
 * Bean Validation, par exemple un etage qui n'existe pas dans le batiment
 * reference : repond en 400 avec le code {@code VALIDATION_ERROR}.
 */
@Getter
public class ValidationException extends ApiException {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super(ApiErrorCode.VALIDATION_ERROR, "La requete contient des donnees invalides");
        this.fieldErrors = new LinkedHashMap<>(fieldErrors);
    }

    public static ValidationException floorOutOfBuilding(String field, int numberOfFloors) {
        return new ValidationException(Map.of(
                field,
                "doit etre compris entre 0 et " + (numberOfFloors - 1) + " dans ce batiment"));
    }
}
