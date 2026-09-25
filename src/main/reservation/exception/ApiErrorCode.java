package reservation.exception;

import org.springframework.http.HttpStatus;

/**
 * Codes d'erreur du contrat HTTP et statut associe.
 *
 * <p>Chaque valeur correspond a une ligne du tableau {@code ApiErrorResponse} de
 * {@code openapi.yml}.</p>
 */
public enum ApiErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INVALID_RESERVATION_PERIOD(HttpStatus.BAD_REQUEST),

    BUILDING_NOT_FOUND(HttpStatus.NOT_FOUND),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORGANIZER_NOT_FOUND(HttpStatus.NOT_FOUND),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND),

    ROOM_CAPACITY_EXCEEDED(HttpStatus.CONFLICT),
    MISSING_REQUIRED_EQUIPMENT(HttpStatus.CONFLICT),
    ROOM_ALREADY_RESERVED(HttpStatus.CONFLICT),
    ROOM_UNAVAILABLE(HttpStatus.CONFLICT),
    NO_COMPATIBLE_ROOM(HttpStatus.CONFLICT),
    RESERVATION_ALREADY_CANCELLED(HttpStatus.CONFLICT),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    BUILDING_FLOOR_COUNT_CONFLICT(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
