package reservation.exception;

import java.util.Map;

/**
 * Ressource referencee par la requete mais absente de la base : repond en 404.
 */
public class ResourceNotFoundException extends ApiException {

    private ResourceNotFoundException(ApiErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    public static ResourceNotFoundException building(Long buildingId) {
        return new ResourceNotFoundException(
                ApiErrorCode.BUILDING_NOT_FOUND,
                "Le batiment " + buildingId + " n'existe pas",
                Map.of("buildingId", buildingId));
    }

    public static ResourceNotFoundException room(Long roomId) {
        return new ResourceNotFoundException(
                ApiErrorCode.ROOM_NOT_FOUND,
                "La salle " + roomId + " n'existe pas",
                Map.of("roomId", roomId));
    }

    public static ResourceNotFoundException organizer(Long organizerId) {
        return new ResourceNotFoundException(
                ApiErrorCode.ORGANIZER_NOT_FOUND,
                "L'organisateur " + organizerId + " n'existe pas",
                Map.of("organizerId", organizerId));
    }

    public static ResourceNotFoundException reservation(Long reservationId) {
        return new ResourceNotFoundException(
                ApiErrorCode.RESERVATION_NOT_FOUND,
                "La reservation " + reservationId + " n'existe pas",
                Map.of("reservationId", reservationId));
    }

    public static ResourceNotFoundException equipment(String equipmentCode) {
        return new ResourceNotFoundException(
                ApiErrorCode.EQUIPMENT_NOT_FOUND,
                "L'equipement " + equipmentCode + " n'existe pas",
                Map.of("equipmentCode", equipmentCode));
    }
}
