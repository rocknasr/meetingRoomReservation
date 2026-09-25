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

    /**
     * @param room salle en maintenance
     * @return l'erreur {@code ROOM_UNAVAILABLE}
     */
    public static ConflictException roomUnavailable(Room room) {
        return new ConflictException(
                ApiErrorCode.ROOM_UNAVAILABLE,
                "La salle " + room.getName() + " est en maintenance",
                Map.of("roomId", room.getId()));
    }

    /**
     * @param room                 salle choisie
     * @param numberOfParticipants nombre de participants demande
     * @return l'erreur {@code ROOM_CAPACITY_EXCEEDED}
     */
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

    /**
     * @param room         salle choisie
     * @param missingCodes codes d'equipement absents de la salle
     * @return l'erreur {@code MISSING_REQUIRED_EQUIPMENT}
     */
    public static ConflictException missingEquipment(Room room, Set<String> missingCodes) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("roomId", room.getId());
        details.put("missingEquipmentCodes", missingCodes);
        return new ConflictException(
                ApiErrorCode.MISSING_REQUIRED_EQUIPMENT,
                "La salle ne possede pas tous les equipements demandes",
                details);
    }

    /**
     * @param room     salle choisie
     * @param conflict reservation confirmee qui chevauche la periode
     * @return l'erreur {@code ROOM_ALREADY_RESERVED}
     */
    public static ConflictException roomAlreadyReserved(Room room, Reservation conflict) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("roomId", room.getId());
        details.put("conflictingReservationId", conflict.getId());
        return new ConflictException(
                ApiErrorCode.ROOM_ALREADY_RESERVED,
                "La salle " + room.getName() + " est deja reservee sur cette periode",
                details);
    }

    /**
     * @return l'erreur {@code NO_COMPATIBLE_ROOM} de l'attribution automatique
     */
    public static ConflictException noCompatibleRoom() {
        return new ConflictException(
                ApiErrorCode.NO_COMPATIBLE_ROOM,
                "Aucune salle disponible ne correspond aux criteres demandes",
                Map.of());
    }

    /**
     * @param reservationId identifiant de la reservation deja annulee
     * @return l'erreur {@code RESERVATION_ALREADY_CANCELLED}
     */
    public static ConflictException reservationAlreadyCancelled(Long reservationId) {
        return new ConflictException(
                ApiErrorCode.RESERVATION_ALREADY_CANCELLED,
                "La reservation " + reservationId + " est deja annulee",
                Map.of("reservationId", reservationId));
    }

    /**
     * @param highestOccupiedFloor etage occupe le plus eleve du batiment
     * @return l'erreur {@code BUILDING_FLOOR_COUNT_CONFLICT}
     */
    public static ConflictException buildingFloorCount(int highestOccupiedFloor) {
        return new ConflictException(
                ApiErrorCode.BUILDING_FLOOR_COUNT_CONFLICT,
                "Une salle ou un organisateur occupe un etage qui serait supprime",
                Map.of("highestOccupiedFloor", highestOccupiedFloor));
    }
}
