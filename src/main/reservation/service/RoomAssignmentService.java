package reservation.service;

import org.springframework.stereotype.Service;

import reservation.model.Organizer;
import reservation.model.Reservation;
import reservation.model.Room;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Selection des salles compatibles et classement pour l'attribution automatique.
 *
 * <p>Ce service ne depend d'aucun repository : il recoit les salles et les
 * reservations a considerer. Il est donc entierement testable sans persistance,
 * ce qui est l'objet des tests unitaires du projet.</p>
 */
@Service
public class RoomAssignmentService {

    // Un etage d'ecart pese autant que dix places inutilisees dans le score.
    public static final int DISTANCE_WEIGHT = 10;

    // Changer de batiment compte comme dix etages d'ecart supplementaires.
    public static final int BUILDING_CHANGE_PENALTY = 10;

    public int computeDistance(Room room, Organizer organizer) {
        int floorGap = Math.abs(room.getFloor() - organizer.getFloor());
        boolean sameBuilding = room.getBuilding().getId().equals(organizer.getBuilding().getId());

        if (sameBuilding) {
            return floorGap;
        }
        return BUILDING_CHANGE_PENALTY + floorGap;
    }

    public int computeUnusedCapacity(Room room, int numberOfParticipants) {
        return room.getCapacity() - numberOfParticipants;
    }

    public long computeScore(Room room, Organizer organizer, int numberOfParticipants) {
        int distance = computeDistance(room, organizer);
        int unusedCapacity = computeUnusedCapacity(room, numberOfParticipants);
        // Plus le score est faible, meilleure est la salle.
        return (long) distance * DISTANCE_WEIGHT + unusedCapacity;
    }

    public boolean isCompatible(
            Room room,
            int numberOfParticipants,
            Set<String> requiredEquipment,
            List<Reservation> reservations,
            Instant start,
            Instant end) {
        if (!room.isAvailable()) {
            return false;
        }
        if (room.getCapacity() < numberOfParticipants) {
            return false;
        }
        if (!room.hasAllEquipment(requiredEquipment)) {
            return false;
        }
        return findConflict(room, reservations, start, end).isEmpty();
    }

    public Optional<Reservation> findConflict(
            Room room, List<Reservation> reservations, Instant start, Instant end) {
        for (Reservation reservation : reservations) {
            boolean sameRoom = reservation.getRoom().equals(room);

            if (sameRoom && reservation.isConfirmed() && reservation.overlaps(start, end)) {
                return Optional.of(reservation);
            }
        }
        return Optional.empty();
    }

    public List<Room> findCompatibleRooms(
            List<Room> rooms,
            int numberOfParticipants,
            Set<String> requiredEquipment,
            List<Reservation> reservations,
            Instant start,
            Instant end) {
        List<Room> compatible = new ArrayList<>();
        for (Room room : rooms) {
            if (isCompatible(room, numberOfParticipants, requiredEquipment, reservations, start, end)) {
                compatible.add(room);
            }
        }
        return compatible;
    }

    public Optional<Room> selectBestRoom(
            List<Room> rooms,
            Organizer organizer,
            int numberOfParticipants,
            Set<String> requiredEquipment,
            List<Reservation> reservations,
            Instant start,
            Instant end) {
        List<Room> compatible = findCompatibleRooms(
                rooms, numberOfParticipants, requiredEquipment, reservations, start, end);

        if (compatible.isEmpty()) {
            return Optional.empty();
        }

        compatible.sort(byScore(organizer, numberOfParticipants));
        return Optional.of(compatible.get(0));
    }

    public List<Room> sortByUnusedCapacity(List<Room> rooms, int requestedCapacity) {
        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort(Comparator
                .comparingInt((Room room) -> computeUnusedCapacity(room, requestedCapacity))
                .thenComparing(byNameThenId()));
        return sorted;
    }

    private Comparator<Room> byScore(Organizer organizer, int numberOfParticipants) {
        return Comparator
                .comparingLong((Room room) -> computeScore(room, organizer, numberOfParticipants))
                .thenComparing(byNameThenId());
    }

    // Departage les egalites pour que le resultat soit toujours le meme.
    private Comparator<Room> byNameThenId() {
        return Comparator
                .comparing(Room::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Room::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
