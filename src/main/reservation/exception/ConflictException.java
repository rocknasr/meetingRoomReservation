package reservation.exception;

import reservation.model.Reservation;
import reservation.model.Room;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Demande coherente mais incompatible avec l'etat courant du systeme : repond en
 * 409. Regroupe les conflits de reservation et le conflit d'etages d'un batiment.
 */
public class ConflictException extends ApiException {

    private ConflictException(ApiErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    public static ConflictException roomUnavailable(Room room) {
        return new ConflictException(
                ApiErrorCode.ROOM_UNAVAILABLE,
                "La salle " + room.getName() + " est en maintenance",
                Map.of("roomId", room.getId()));
    }

    public static ConflictException roomCapacityExceeded(Room room, int numberOfParticipants) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("roomId", room.getId());
        details.put("roomCapacity", room.getCapacity());
        details.put("numberOfParticipants", numberOfParticipants);
        return new ConflictException(
                ApiErrorCode.ROOM_CAPACITY_EXCEEDED,
                "La capacite de la salle est insuffisante",
                details);
    }

    public static ConflictException missingEquipment(Room room, Set<String> missingCodes) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("roomId", room.getId());
        details.put("missingEquipmentCodes", missingCodes);
        return new ConflictException(
                ApiErrorCode.MISSING_REQUIRED_EQUIPMENT,
                "La salle ne possede pas tous les equipements demandes",
                details);
    }

    public static ConflictException roomAlreadyReserved(Room room, Reservation conflict) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("roomId", room.getId());
        details.put("conflictingReservationId", conflict.getId());
        return new ConflictException(
                ApiErrorCode.ROOM_ALREADY_RESERVED,
                "La salle " + room.getName() + " est deja reservee sur cette periode",
                details);
    }

    public static ConflictException noCompatibleRoom() {
        return new ConflictException(
                ApiErrorCode.NO_COMPATIBLE_ROOM,
                "Aucune salle disponible ne correspond aux criteres demandes",
                Map.of());
    }

    public static ConflictException reservationAlreadyCancelled(Long reservationId) {
        return new ConflictException(
                ApiErrorCode.RESERVATION_ALREADY_CANCELLED,
                "La reservation " + reservationId + " est deja annulee",
                Map.of("reservationId", reservationId));
    }

    public static ConflictException buildingFloorCount(int highestOccupiedFloor) {
        return new ConflictException(
                ApiErrorCode.BUILDING_FLOOR_COUNT_CONFLICT,
                "Une salle ou un organisateur occupe un etage qui serait supprime",
                Map.of("highestOccupiedFloor", highestOccupiedFloor));
    }
}
