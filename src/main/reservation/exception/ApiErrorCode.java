package reservation.exception;

import org.springframework.http.HttpStatus;

/**
 * Codes d'erreur du contrat HTTP et statut associe.
 *
 * <p>Chaque valeur correspond a une ligne du tableau {@code ApiErrorResponse} de
 * {@code openapi.yml}.</p>
 */
public enum ApiErrorCode {

    /** Un ou plusieurs champs sont invalides. */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    /** La periode est passee, inversee ou depasse huit heures. */
    INVALID_RESERVATION_PERIOD(HttpStatus.BAD_REQUEST),

    BUILDING_NOT_FOUND(HttpStatus.NOT_FOUND),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORGANIZER_NOT_FOUND(HttpStatus.NOT_FOUND),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** La salle choisie est trop petite. */
    ROOM_CAPACITY_EXCEEDED(HttpStatus.CONFLICT),
    /** La salle choisie ne possede pas tous les equipements demandes. */
    MISSING_REQUIRED_EQUIPMENT(HttpStatus.CONFLICT),
    /** Une reservation confirmee chevauche la periode. */
    ROOM_ALREADY_RESERVED(HttpStatus.CONFLICT),
    /** La salle choisie est en maintenance. */
    ROOM_UNAVAILABLE(HttpStatus.CONFLICT),
    /** Aucune salle ne convient a l'attribution automatique. */
    NO_COMPATIBLE_ROOM(HttpStatus.CONFLICT),
    /** La reservation est deja annulee. */
    RESERVATION_ALREADY_CANCELLED(HttpStatus.CONFLICT),
    /** Un nom, code ou e-mail unique est deja utilise. */
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    /** Le nombre d'etages est incompatible avec une localisation existante. */
    BUILDING_FLOOR_COUNT_CONFLICT(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    /**
     * @return le statut HTTP a renvoyer pour ce code d'erreur
     */
    public HttpStatus getStatus() {
        return status;
    }
}
