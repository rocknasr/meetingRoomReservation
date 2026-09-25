package reservation.service;

import reservation.model.Building;
import reservation.model.Equipment;
import reservation.model.Organizer;
import reservation.model.Reservation;
import reservation.model.ReservationStatus;
import reservation.model.Room;
import reservation.model.RoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires du classement et de l'attribution automatique des salles.
 *
 * <p>Ces tests n'utilisent ni base de donnees ni contexte Spring : le service
 * recoit directement les salles et les reservations a considerer.</p>
 */
class RoomAssignmentServiceTest {

    private static final Instant START = Instant.parse("2026-10-15T14:00:00Z");
    private static final Instant END = Instant.parse("2026-10-15T16:00:00Z");

    private RoomAssignmentService roomAssignmentService;

    private Building buildingA;
    private Building buildingB;
    private Organizer organizer;

    @BeforeEach
    void setUp() {
        roomAssignmentService = new RoomAssignmentService();

        buildingA = building(1L, "Batiment A", 5);
        buildingB = building(2L, "Batiment B", 5);
        // L'organisateur travaille au 2e etage du batiment A.
        organizer = organizer(1L, buildingA, 2);
    }

    @Test
    @DisplayName("Une salle dont la capacite est exactement suffisante est retenue")
    void shouldAcceptRoomWithExactlySufficientCapacity() {
        // GIVEN une salle de 30 places et une reunion de 30 participants
        Room room = room(1L, "Orion", buildingA, 2, 30);

        // WHEN on verifie sa compatibilite
        boolean compatible = roomAssignmentService.isCompatible(
                room, 30, Set.of(), List.of(), START, END);

        // THEN la salle est retenue : une salle de 30 places accueille 30 personnes
        assertTrue(compatible);
    }

    @Test
    @DisplayName("Une salle trop petite est rejetee")
    void shouldRejectRoomThatIsTooSmall() {
        // GIVEN une salle de 20 places et une reunion de 21 participants
        Room room = room(1L, "Orion", buildingA, 2, 20);

        // WHEN on verifie sa compatibilite
        boolean compatible = roomAssignmentService.isCompatible(
                room, 21, Set.of(), List.of(), START, END);

        // THEN la salle est rejetee
        assertFalse(compatible);
    }

    @Test
    @DisplayName("Une salle en maintenance est rejetee")
    void shouldRejectRoomInMaintenance() {
        // GIVEN une salle assez grande mais en maintenance
        Room room = room(1L, "Orion", buildingA, 2, 30);
        room.setStatus(RoomStatus.MAINTENANCE);

        // WHEN on verifie sa compatibilite
        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of(), List.of(), START, END);

        // THEN la salle est rejetee
        assertFalse(compatible);
    }

    @Test
    @DisplayName("Une salle deja reservee sur la periode est rejetee")
    void shouldRejectRoomAlreadyReserved() {
        // GIVEN une salle occupee par une reservation confirmee qui chevauche la periode
        Room room = room(1L, "Orion", buildingA, 2, 30);
        Reservation existing = reservation(
                room, Instant.parse("2026-10-15T15:00:00Z"), Instant.parse("2026-10-15T17:00:00Z"));

        // WHEN on verifie sa compatibilite
        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of(), List.of(existing), START, END);

        // THEN la salle est rejetee
        assertFalse(compatible);
    }

    @Test
    @DisplayName("Une reservation annulee ne bloque plus la salle")
    void shouldIgnoreCancelledReservation() {
        // GIVEN une salle dont la seule reservation chevauchante est annulee
        Room room = room(1L, "Orion", buildingA, 2, 30);
        Reservation cancelled = reservation(
                room, Instant.parse("2026-10-15T15:00:00Z"), Instant.parse("2026-10-15T17:00:00Z"));
        cancelled.setStatus(ReservationStatus.CANCELLED);

        // WHEN on verifie sa compatibilite
        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of(), List.of(cancelled), START, END);

        // THEN la salle reste disponible
        assertTrue(compatible);
    }

    @Test
    @DisplayName("Une salle sans un equipement demande est rejetee")
    void shouldRejectRoomWithoutRequiredEquipment() {
        // GIVEN une salle equipee d'un tableau blanc seulement
        Room room = room(1L, "Orion", buildingA, 2, 30);
        room.setEquipment(equipment("WHITEBOARD"));

        // WHEN on exige un videoprojecteur
        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of("PROJECTOR"), List.of(), START, END);

        // THEN la salle est rejetee
        assertFalse(compatible);
    }

    @Test
    @DisplayName("Un equipement supplementaire n'empeche pas la selection")
    void shouldAcceptRoomWithExtraEquipment() {
        // GIVEN une salle equipee d'un videoprojecteur et d'un tableau blanc
        Room room = room(1L, "Orion", buildingA, 2, 30);
        room.setEquipment(equipment("PROJECTOR", "WHITEBOARD"));

        // WHEN on exige seulement le videoprojecteur
        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of("PROJECTOR"), List.of(), START, END);

        // THEN la salle est retenue
        assertTrue(compatible);
    }

    @Test
    @DisplayName("Deux reservations consecutives sont acceptees")
    void shouldAcceptConsecutiveReservations() {
        // GIVEN une salle reservee de 12:00 a 14:00
        Room room = room(1L, "Orion", buildingA, 2, 30);
        Reservation earlier = reservation(
                room, Instant.parse("2026-10-15T12:00:00Z"), START);

        // WHEN on demande la periode 14:00 - 16:00, qui commence a la fin de la precedente
        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of(), List.of(earlier), START, END);

        // THEN la salle est disponible : la fin d'une reservation est exclue
        assertTrue(compatible);
    }

    @Test
    @DisplayName("Dans un meme batiment, la distance est l'ecart d'etages")
    void shouldComputeDistanceInsideSameBuilding() {
        // GIVEN une salle au 4e etage du batiment de l'organisateur, qui est au 2e
        Room room = room(1L, "Orion", buildingA, 4, 30);

        // WHEN on calcule la distance
        int distance = roomAssignmentService.computeDistance(room, organizer);

        // THEN elle vaut l'ecart d'etages
        assertEquals(2, distance);
    }

    @Test
    @DisplayName("Dans un autre batiment, la distance ajoute une penalite de dix etages")
    void shouldComputeDistanceBetweenTwoBuildings() {
        // GIVEN une salle au 4e etage d'un autre batiment
        Room room = room(1L, "Orion", buildingB, 4, 30);

        // WHEN on calcule la distance
        int distance = roomAssignmentService.computeDistance(room, organizer);

        // THEN la penalite de changement de batiment s'ajoute a l'ecart d'etages
        assertEquals(12, distance);
    }

    @Test
    @DisplayName("Le score combine la distance et les places inutilisees")
    void shouldComputeScoreFromDistanceAndUnusedCapacity() {
        // GIVEN une salle de 30 places au 3e etage du meme batiment, pour 25 participants
        Room room = room(1L, "Orion", buildingA, 3, 30);

        // WHEN on calcule le score
        long score = roomAssignmentService.computeScore(room, organizer, 25);

        // THEN score = distance * 10 + placesInutilisees = 1 * 10 + 5
        assertEquals(15L, score);
    }

    @Test
    @DisplayName("La salle au score le plus faible est selectionnee")
    void shouldSelectRoomWithLowestScore() {
        // GIVEN une salle proche mais surdimensionnee et une salle juste au bon etage
        Room farRoom = room(1L, "Lointaine", buildingA, 4, 100);   // score = 20 + 75 = 95
        Room bestRoom = room(2L, "Proche", buildingA, 2, 30);      // score = 0 + 5 = 5

        // WHEN on demande la meilleure salle pour 25 participants
        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(farRoom, bestRoom), organizer, 25, Set.of(), List.of(), START, END);

        // THEN la salle au score le plus faible est retenue
        assertTrue(selected.isPresent());
        assertEquals(bestRoom, selected.get());
    }

    @Test
    @DisplayName("Un etage de distance equivaut a dix places inutilisees")
    void shouldPreferCloserRoomWhenScoresDiffer() {
        // GIVEN une salle a un etage d'ecart sans place perdue et une salle sur place
        // avec onze places perdues
        Room closerRoom = room(1L, "Alpha", buildingA, 3, 10);   // score = 10 + 0 = 10
        Room sameFloorRoom = room(2L, "Beta", buildingA, 2, 21); // score = 0 + 11 = 11

        // WHEN on demande la meilleure salle pour 10 participants
        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(sameFloorRoom, closerRoom), organizer, 10, Set.of(), List.of(), START, END);

        // THEN la salle a l'etage voisin l'emporte, son score etant plus faible
        assertEquals(closerRoom, selected.orElseThrow());
    }

    @Test
    @DisplayName("A score egal, les salles sont departagees par leur nom")
    void shouldBreakScoreTieByName() {
        // GIVEN deux salles identiques, dont les noms different par la casse et l'ordre
        Room zeta = room(1L, "zeta", buildingA, 2, 30);
        Room alpha = room(2L, "Alpha", buildingA, 2, 30);

        // WHEN on demande la meilleure salle
        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(zeta, alpha), organizer, 25, Set.of(), List.of(), START, END);

        // THEN la premiere dans l'ordre alphabetique est retenue, sans tenir compte de la casse
        assertEquals(alpha, selected.orElseThrow());
    }

    @Test
    @DisplayName("A score et nom egaux, les salles sont departagees par identifiant")
    void shouldBreakNameTieById() {
        // GIVEN deux salles de meme score portant le meme nom a la casse pres
        Room second = room(7L, "Orion", buildingA, 2, 30);
        Room first = room(3L, "orion", buildingA, 2, 30);

        // WHEN on demande la meilleure salle
        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(second, first), organizer, 25, Set.of(), List.of(), START, END);

        // THEN le plus petit identifiant est retenu, le resultat reste deterministe
        assertEquals(first, selected.orElseThrow());
    }

    @Test
    @DisplayName("Aucune salle n'est attribuee lorsque aucune n'est compatible")
    void shouldReturnEmptyWhenNoRoomIsCompatible() {
        // GIVEN une salle trop petite et une salle en maintenance
        Room tooSmall = room(1L, "Alpha", buildingA, 2, 5);
        Room inMaintenance = room(2L, "Beta", buildingA, 2, 50);
        inMaintenance.setStatus(RoomStatus.MAINTENANCE);

        // WHEN on demande la meilleure salle pour 25 participants
        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(tooSmall, inMaintenance), organizer, 25, Set.of(), List.of(), START, END);

        // THEN aucune salle n'est proposee et aucune reservation ne peut etre creee
        assertTrue(selected.isEmpty());
    }

    @Test
    @DisplayName("La recherche de disponibilite classe par places inutilisees, nom puis identifiant")
    void shouldSortAvailableRoomsByUnusedCapacityThenNameThenId() {
        // GIVEN trois salles, dont deux de meme capacite
        Room large = room(1L, "Alpha", buildingA, 2, 50);
        Room exactBeta = room(2L, "Beta", buildingA, 2, 30);
        Room exactAlpha = room(3L, "aphelie", buildingA, 2, 30);

        // WHEN on les classe pour une reunion de 25 participants
        List<Room> sorted = roomAssignmentService.sortByUnusedCapacity(
                List.of(large, exactBeta, exactAlpha), 25);

        // THEN les salles les plus justes viennent d'abord, departagees par nom
        assertEquals(List.of(exactAlpha, exactBeta, large), sorted);
    }

    private Building building(Long id, String name, int numberOfFloors) {
        Building building = new Building(name, numberOfFloors);
        building.setId(id);
        return building;
    }

    private Organizer organizer(Long id, Building building, int floor) {
        Organizer organizer = new Organizer("Alice Martin", "alice@example.org", building, floor);
        organizer.setId(id);
        return organizer;
    }

    private Room room(Long id, String name, Building building, int floor, int capacity) {
        Room room = new Room(name, building, floor, capacity);
        room.setId(id);
        return room;
    }

    private Set<Equipment> equipment(String... codes) {
        Set<Equipment> equipment = new LinkedHashSet<>();
        long id = 1L;
        for (String code : codes) {
            Equipment item = new Equipment(code, code);
            item.setId(id++);
            equipment.add(item);
        }
        return equipment;
    }

    private Reservation reservation(Room room, Instant start, Instant end) {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setTitle("Reunion");
        reservation.setRoom(room);
        reservation.setStart(start);
        reservation.setEnd(end);
        reservation.setNumberOfParticipants(5);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(Instant.parse("2026-10-01T10:00:00Z"));
        return reservation;
    }
}
