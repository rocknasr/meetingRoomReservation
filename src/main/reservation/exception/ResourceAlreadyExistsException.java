package reservation.exception;

/**
 * Violation d'une contrainte d'unicite (nom de batiment ou de salle, code
 * d'equipement, adresse e-mail) : repond en 409.
 */
public class ResourceAlreadyExistsException extends ApiException {

    public ResourceAlreadyExistsException(String message) {
        super(ApiErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }

    public static ResourceAlreadyExistsException building() {
        return new ResourceAlreadyExistsException("Un batiment utilise deja ce nom");
    }

    public static ResourceAlreadyExistsException room() {
        return new ResourceAlreadyExistsException("Une salle utilise deja ce nom");
    }

    public static ResourceAlreadyExistsException equipment() {
        return new ResourceAlreadyExistsException("Un equipement utilise deja ce code");
    }

    public static ResourceAlreadyExistsException organizer() {
        return new ResourceAlreadyExistsException("Un organisateur utilise deja cette adresse e-mail");
    }
}
