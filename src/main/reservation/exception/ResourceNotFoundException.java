package reservation.exception;

import java.util.Map;

/**
 * Ressource referencee par la requete mais absente de la base : repond en 404.
 */
public class ResourceNotFoundException extends ApiException {

    private ResourceNotFoundException(ApiErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    /**
     * @param buildingId identifiant demande
     * @return l'erreur {@code BUILDING_NOT_FOUND}
     */
    public static ResourceNotFoundException building(Long buildingId) {
        return new ResourceNotFoundException(
                ApiErrorCode.BUILDING_NOT_FOUND,
                "Le batiment " + buildingId + " n'existe pas",
                Map.of("buildingId", buildingId));
    }

    /**
     * @param roomId identifiant demande
     * @return l'erreur {@code ROOM_NOT_FOUND}
     */
    public static ResourceNotFoundException room(Long roomId) {
        return new ResourceNotFoundException(
                ApiErrorCode.ROOM_NOT_FOUND,
                "La salle " + roomId + " n'existe pas",
                Map.of("roomId", roomId));
    }

    /**
     * @param organizerId identifiant demande
     * @return l'erreur {@code ORGANIZER_NOT_FOUND}
     */
    public static ResourceNotFoundException organizer(Long organizerId) {
        return new ResourceNotFoundException(
                ApiErrorCode.ORGANIZER_NOT_FOUND,
                "L'organisateur " + organizerId + " n'existe pas",
                Map.of("organizerId", organizerId));
    }

    /**
     * @param reservationId identifiant demande
     * @return l'erreur {@code RESERVATION_NOT_FOUND}
     */
    public static ResourceNotFoundException reservation(Long reservationId) {
        return new ResourceNotFoundException(
                ApiErrorCode.RESERVATION_NOT_FOUND,
                "La reservation " + reservationId + " n'existe pas",
                Map.of("reservationId", reservationId));
    }

    /**
     * @param equipmentCode code d'equipement demande
     * @return l'erreur {@code EQUIPMENT_NOT_FOUND}
     */
    public static ResourceNotFoundException equipment(String equipmentCode) {
        return new ResourceNotFoundException(
                ApiErrorCode.EQUIPMENT_NOT_FOUND,
                "L'equipement " + equipmentCode + " n'existe pas",
                Map.of("equipmentCode", equipmentCode));
    }
}
