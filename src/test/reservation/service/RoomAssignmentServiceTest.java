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
        organizer = organizer(1L, buildingA, 2);
    }

    @Test
    @DisplayName("Une salle dont la capacite est exactement suffisante est retenue")
    void shouldAcceptRoomWithExactlySufficientCapacity() {
        Room room = room(1L, "Orion", buildingA, 2, 30);

        boolean compatible = roomAssignmentService.isCompatible(
                room, 30, Set.of(), List.of(), START, END);

        assertTrue(compatible);
    }

    @Test
    @DisplayName("Une salle trop petite est rejetee")
    void shouldRejectRoomThatIsTooSmall() {
        Room room = room(1L, "Orion", buildingA, 2, 20);

        boolean compatible = roomAssignmentService.isCompatible(
                room, 21, Set.of(), List.of(), START, END);

        assertFalse(compatible);
    }

    @Test
    @DisplayName("Une salle en maintenance est rejetee")
    void shouldRejectRoomInMaintenance() {
        Room room = room(1L, "Orion", buildingA, 2, 30);
        room.setStatus(RoomStatus.MAINTENANCE);

        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of(), List.of(), START, END);

        assertFalse(compatible);
    }

    @Test
    @DisplayName("Une salle deja reservee sur la periode est rejetee")
    void shouldRejectRoomAlreadyReserved() {
        Room room = room(1L, "Orion", buildingA, 2, 30);
        Reservation existing = reservation(
                room, Instant.parse("2026-10-15T15:00:00Z"), Instant.parse("2026-10-15T17:00:00Z"));

        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of(), List.of(existing), START, END);

        assertFalse(compatible);
    }

    @Test
    @DisplayName("Une reservation annulee ne bloque plus la salle")
    void shouldIgnoreCancelledReservation() {
        Room room = room(1L, "Orion", buildingA, 2, 30);
        Reservation cancelled = reservation(
                room, Instant.parse("2026-10-15T15:00:00Z"), Instant.parse("2026-10-15T17:00:00Z"));
        cancelled.setStatus(ReservationStatus.CANCELLED);

        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of(), List.of(cancelled), START, END);

        assertTrue(compatible);
    }

    @Test
    @DisplayName("Une salle sans un equipement demande est rejetee")
    void shouldRejectRoomWithoutRequiredEquipment() {
        Room room = room(1L, "Orion", buildingA, 2, 30);
        room.setEquipment(equipment("WHITEBOARD"));

        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of("PROJECTOR"), List.of(), START, END);

        assertFalse(compatible);
    }

    @Test
    @DisplayName("Un equipement supplementaire n'empeche pas la selection")
    void shouldAcceptRoomWithExtraEquipment() {
        Room room = room(1L, "Orion", buildingA, 2, 30);
        room.setEquipment(equipment("PROJECTOR", "WHITEBOARD"));

        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of("PROJECTOR"), List.of(), START, END);

        assertTrue(compatible);
    }

    @Test
    @DisplayName("Deux reservations consecutives sont acceptees")
    void shouldAcceptConsecutiveReservations() {
        Room room = room(1L, "Orion", buildingA, 2, 30);
        Reservation earlier = reservation(
                room, Instant.parse("2026-10-15T12:00:00Z"), START);

        boolean compatible = roomAssignmentService.isCompatible(
                room, 10, Set.of(), List.of(earlier), START, END);

        assertTrue(compatible);
    }

    @Test
    @DisplayName("Dans un meme batiment, la distance est l'ecart d'etages")
    void shouldComputeDistanceInsideSameBuilding() {
        Room room = room(1L, "Orion", buildingA, 4, 30);

        int distance = roomAssignmentService.computeDistance(room, organizer);

        assertEquals(2, distance);
    }

    @Test
    @DisplayName("Dans un autre batiment, la distance ajoute une penalite de dix etages")
    void shouldComputeDistanceBetweenTwoBuildings() {
        Room room = room(1L, "Orion", buildingB, 4, 30);

        int distance = roomAssignmentService.computeDistance(room, organizer);

        assertEquals(12, distance);
    }

    @Test
    @DisplayName("Le score combine la distance et les places inutilisees")
    void shouldComputeScoreFromDistanceAndUnusedCapacity() {
        Room room = room(1L, "Orion", buildingA, 3, 30);

        long score = roomAssignmentService.computeScore(room, organizer, 25);

        // Distance 1 etage x 10 + 5 places inutilisees.
        assertEquals(15L, score);
    }

    @Test
    @DisplayName("La salle au score le plus faible est selectionnee")
    void shouldSelectRoomWithLowestScore() {
        Room farRoom = room(1L, "Lointaine", buildingA, 4, 100);
        Room bestRoom = room(2L, "Proche", buildingA, 2, 30);

        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(farRoom, bestRoom), organizer, 25, Set.of(), List.of(), START, END);

        assertTrue(selected.isPresent());
        assertEquals(bestRoom, selected.get());
    }

    @Test
    @DisplayName("Un etage de distance equivaut a dix places inutilisees")
    void shouldPreferCloserRoomWhenScoresDiffer() {
        // Alpha : 1 x 10 + 0 = 10 ; Beta : 0 x 10 + 11 = 11.
        Room closerRoom = room(1L, "Alpha", buildingA, 3, 10);
        Room sameFloorRoom = room(2L, "Beta", buildingA, 2, 21);

        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(sameFloorRoom, closerRoom), organizer, 10, Set.of(), List.of(), START, END);

        assertEquals(closerRoom, selected.orElseThrow());
    }

    @Test
    @DisplayName("A score egal, les salles sont departagees par leur nom")
    void shouldBreakScoreTieByName() {
        Room zeta = room(1L, "zeta", buildingA, 2, 30);
        Room alpha = room(2L, "Alpha", buildingA, 2, 30);

        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(zeta, alpha), organizer, 25, Set.of(), List.of(), START, END);

        assertEquals(alpha, selected.orElseThrow());
    }

    @Test
    @DisplayName("A score et nom egaux, les salles sont departagees par identifiant")
    void shouldBreakNameTieById() {
        Room second = room(7L, "Orion", buildingA, 2, 30);
        Room first = room(3L, "orion", buildingA, 2, 30);

        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(second, first), organizer, 25, Set.of(), List.of(), START, END);

        assertEquals(first, selected.orElseThrow());
    }

    @Test
    @DisplayName("Aucune salle n'est attribuee lorsque aucune n'est compatible")
    void shouldReturnEmptyWhenNoRoomIsCompatible() {
        Room tooSmall = room(1L, "Alpha", buildingA, 2, 5);
        Room inMaintenance = room(2L, "Beta", buildingA, 2, 50);
        inMaintenance.setStatus(RoomStatus.MAINTENANCE);

        Optional<Room> selected = roomAssignmentService.selectBestRoom(
                List.of(tooSmall, inMaintenance), organizer, 25, Set.of(), List.of(), START, END);

        assertTrue(selected.isEmpty());
    }

    @Test
    @DisplayName("La recherche de disponibilite classe par places inutilisees, nom puis identifiant")
    void shouldSortAvailableRoomsByUnusedCapacityThenNameThenId() {
        Room large = room(1L, "Alpha", buildingA, 2, 50);
        Room exactBeta = room(2L, "Beta", buildingA, 2, 30);
        Room exactAlpha = room(3L, "aphelie", buildingA, 2, 30);

        List<Room> sorted = roomAssignmentService.sortByUnusedCapacity(
                List.of(large, exactBeta, exactAlpha), 25);

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
