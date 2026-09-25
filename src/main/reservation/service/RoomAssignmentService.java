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

    /** Un etage de distance equivaut a dix places inutilisees. */
    public static final int DISTANCE_WEIGHT = 10;

    /** Changer de batiment coute dix etages, soit cent points de score. */
    public static final int BUILDING_CHANGE_PENALTY = 10;

    /**
     * Calcule la distance entre un organisateur et une salle.
     *
     * <p>Dans un meme batiment, la distance est l'ecart d'etages. Dans deux
     * batiments differents, une penalite de dix etages s'y ajoute.</p>
     *
     * @param room      salle candidate
     * @param organizer organisateur a l'origine de la demande
     * @return la distance, toujours positive ou nulle
     */
    public int computeDistance(Room room, Organizer organizer) {
        int floorGap = Math.abs(room.getFloor() - organizer.getFloor());
        boolean sameBuilding = room.getBuilding().getId().equals(organizer.getBuilding().getId());

        if (sameBuilding) {
            return floorGap;
        }
        return BUILDING_CHANGE_PENALTY + floorGap;
    }

    /**
     * Calcule le nombre de places qui resteraient inutilisees dans la salle.
     *
     * @param room                 salle candidate
     * @param numberOfParticipants nombre de participants attendus
     * @return la difference entre la capacite et le nombre de participants
     */
    public int computeUnusedCapacity(Room room, int numberOfParticipants) {
        return room.getCapacity() - numberOfParticipants;
    }

    /**
     * Calcule le score d'une salle : distance * 10 + places inutilisees.
     * Le score le plus faible designe la meilleure salle.
     *
     * @param room                 salle candidate
     * @param organizer            organisateur a l'origine de la demande
     * @param numberOfParticipants nombre de participants attendus
     * @return le score de la salle
     */
    public long computeScore(Room room, Organizer organizer, int numberOfParticipants) {
        int distance = computeDistance(room, organizer);
        int unusedCapacity = computeUnusedCapacity(room, numberOfParticipants);
        return (long) distance * DISTANCE_WEIGHT + unusedCapacity;
    }

    /**
     * Verifie qu'une salle peut accueillir la reunion demandee.
     *
     * <p>La salle doit etre disponible, assez grande, equipee de tout ce qui est
     * exige et libre sur la periode. Les reservations annulees sont ignorees.</p>
     *
     * @param room                 salle candidate
     * @param numberOfParticipants nombre de participants attendus
     * @param requiredEquipment    codes des equipements exiges
     * @param reservations         reservations connues, tous statuts et salles confondus
     * @param start                debut de la periode demandee
     * @param end                  fin de la periode demandee
     * @return vrai lorsque toutes les conditions sont remplies
     */
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

    /**
     * Recherche une reservation confirmee de la salle qui chevauche la periode.
     *
     * @param room         salle candidate
     * @param reservations reservations connues, tous statuts et salles confondus
     * @param start        debut de la periode demandee
     * @param end          fin de la periode demandee
     * @return la premiere reservation en conflit, s'il en existe une
     */
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

    /**
     * Ne conserve que les salles compatibles avec la demande.
     *
     * @param rooms                salles a examiner
     * @param numberOfParticipants nombre de participants attendus
     * @param requiredEquipment    codes des equipements exiges
     * @param reservations         reservations connues
     * @param start                debut de la periode demandee
     * @param end                  fin de la periode demandee
     * @return les salles compatibles, dans l'ordre recu
     */
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

    /**
     * Choisit la salle la mieux adaptee a la demande.
     *
     * <p>Les salles compatibles sont classees par score croissant, puis, a egalite,
     * par nom sans tenir compte de la casse et enfin par identifiant : le resultat
     * est donc deterministe.</p>
     *
     * @param rooms                salles a examiner
     * @param organizer            organisateur a l'origine de la demande
     * @param numberOfParticipants nombre de participants attendus
     * @param requiredEquipment    codes des equipements exiges
     * @param reservations         reservations connues
     * @param start                debut de la periode demandee
     * @param end                  fin de la periode demandee
     * @return la meilleure salle, ou un optionnel vide si aucune n'est compatible
     */
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

    /**
     * Classe les salles compatibles pour la recherche de disponibilite : places
     * inutilisees croissantes, puis nom, puis identifiant.
     *
     * @param rooms             salles compatibles
     * @param requestedCapacity nombre de participants attendus
     * @return les salles triees
     */
    public List<Room> sortByUnusedCapacity(List<Room> rooms, int requestedCapacity) {
        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort(Comparator
                .comparingInt((Room room) -> computeUnusedCapacity(room, requestedCapacity))
                .thenComparing(byNameThenId()));
        return sorted;
    }

    /**
     * Comparateur d'attribution automatique : score, puis nom, puis identifiant.
     */
    private Comparator<Room> byScore(Organizer organizer, int numberOfParticipants) {
        return Comparator
                .comparingLong((Room room) -> computeScore(room, organizer, numberOfParticipants))
                .thenComparing(byNameThenId());
    }

    /**
     * Departage deux salles de meme score : nom alphabetique sans tenir compte de
     * la casse, puis identifiant.
     */
    private Comparator<Room> byNameThenId() {
        return Comparator
                .comparing(Room::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Room::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
