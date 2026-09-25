package reservation.controller;

import reservation.dto.ReservationRequest;
import reservation.model.Building;
import reservation.model.Organizer;
import reservation.model.Room;
import reservation.model.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration des endpoints de reservation : reservation directe,
 * attribution automatique, conflits et annulation.
 */
class ReservationControllerTest extends AbstractControllerTest {

    @Test
    @DisplayName("Une reservation creee est confirmee et consultable")
    void shouldCreateAndReadReservation() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        ReservationRequest request = new ReservationRequest(
                "Soutenance", organizer.getId(), start, start.plusHours(2), 25, Set.of(), room.getId());

        String location = mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.title").value("Soutenance"))
                .andExpect(jsonPath("$.room.id").value(room.getId()))
                .andExpect(jsonPath("$.organizer.id").value(organizer.getId()))
                .andExpect(jsonPath("$.numberOfParticipants").value(25))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("L'attribution automatique retient la salle au score le plus faible")
    void shouldAssignRoomAutomatically() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        givenRoom("Grande", building, 2, 100);
        Room best = givenRoom("Juste", building, 3, 25);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        ReservationRequest request = new ReservationRequest(
                "Comite", organizer.getId(), start, start.plusHours(2), 25, Set.of(), null);

        mockMvc.perform(post("/api/reservations/automatic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.room.id").value(best.getId()))
                .andExpect(jsonPath("$.room.name").value("Juste"));
    }

    @Test
    @DisplayName("Sans salle compatible, aucune reservation n'est creee")
    void shouldRejectAutomaticReservationWhenNoRoomIsCompatible() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        givenRoom("Petite", building, 2, 5);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        ReservationRequest request = new ReservationRequest(
                "Comite", organizer.getId(), start, start.plusHours(2), 25, Set.of(), null);

        mockMvc.perform(post("/api/reservations/automatic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_COMPATIBLE_ROOM"))
                .andExpect(jsonPath("$.path").value("/api/reservations/automatic"));

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Une reservation qui en chevauche une autre est refusee")
    void shouldRejectOverlappingReservation() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        reserve(organizer, room, start, start.plusHours(2))
                .andExpect(status().isCreated());

        reserve(organizer, room, start.plusHours(1), start.plusHours(3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_ALREADY_RESERVED"))
                .andExpect(jsonPath("$.details.roomId").value(room.getId()))
                .andExpect(jsonPath("$.details.conflictingReservationId").exists());
    }

    @Test
    @DisplayName("Deux reservations consecutives sont acceptees")
    void shouldAcceptConsecutiveReservations() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        reserve(organizer, room, start, start.plusHours(1))
                .andExpect(status().isCreated());

        reserve(organizer, room, start.plusHours(1), start.plusHours(2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Apres annulation, la salle est de nouveau reservable sur la meme periode")
    void shouldAllowNewReservationAfterCancellation() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        String location = reserve(organizer, room, start, start.plusHours(2))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(patch(location + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        reserve(organizer, room, start, start.plusHours(2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Annuler deux fois renvoie RESERVATION_ALREADY_CANCELLED")
    void shouldRejectSecondCancellation() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        String location = reserve(organizer, room, start, start.plusHours(2))
                .andReturn().getResponse().getHeader("Location");
        mockMvc.perform(patch(location + "/cancel")).andExpect(status().isOk());

        mockMvc.perform(patch(location + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_ALREADY_CANCELLED"));
    }

    @Test
    @DisplayName("Une salle en maintenance ne peut pas etre reservee")
    void shouldRejectReservationOnRoomInMaintenance() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        room.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.saveAndFlush(room);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();

        reserve(organizer, room, start, start.plusHours(2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_UNAVAILABLE"))
                .andExpect(jsonPath("$.details.roomId").value(room.getId()));
    }

    @Test
    @DisplayName("Une salle trop petite renvoie ROOM_CAPACITY_EXCEEDED")
    void shouldRejectReservationExceedingCapacity() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 20);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        ReservationRequest request = new ReservationRequest(
                "Soutenance", organizer.getId(), start, start.plusHours(2), 25, Set.of(), room.getId());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_CAPACITY_EXCEEDED"))
                .andExpect(jsonPath("$.details.roomCapacity").value(20))
                .andExpect(jsonPath("$.details.numberOfParticipants").value(25));
    }

    @Test
    @DisplayName("Une salle sans l'equipement demande renvoie MISSING_REQUIRED_EQUIPMENT")
    void shouldRejectReservationWithMissingEquipment() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        givenEquipment("PROJECTOR", "Videoprojecteur");
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        ReservationRequest request = new ReservationRequest(
                "Soutenance", organizer.getId(), start, start.plusHours(2), 10,
                Set.of("PROJECTOR"), room.getId());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_EQUIPMENT"))
                .andExpect(jsonPath("$.details.missingEquipmentCodes[0]").value("PROJECTOR"));
    }

    @Test
    @DisplayName("Une periode de plus de huit heures renvoie INVALID_RESERVATION_PERIOD")
    void shouldRejectReservationLongerThanEightHours() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();

        reserve(organizer, room, start, start.plusHours(9))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_PERIOD"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/reservations"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    @DisplayName("Une periode passee renvoie INVALID_RESERVATION_PERIOD")
    void shouldRejectReservationStartingInThePast() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart().minusDays(14);

        reserve(organizer, room, start, start.plusHours(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_PERIOD"));
    }

    @Test
    @DisplayName("Un nombre de participants nul renvoie VALIDATION_ERROR")
    void shouldRejectReservationWithoutParticipants() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        ReservationRequest request = new ReservationRequest(
                "Soutenance", organizer.getId(), start, start.plusHours(2), 0, Set.of(), room.getId());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.numberOfParticipants").exists());
    }

    @Test
    @DisplayName("Une reservation sans salle choisie renvoie VALIDATION_ERROR")
    void shouldRejectReservationWithoutRoom() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        givenRoom("Orion", building, 2, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        ReservationRequest request = new ReservationRequest(
                "Soutenance", organizer.getId(), start, start.plusHours(2), 10, Set.of(), null);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.roomId").exists());
    }

    @Test
    @DisplayName("Un organisateur inconnu renvoie ORGANIZER_NOT_FOUND")
    void shouldRejectReservationWithUnknownOrganizer() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);
        OffsetDateTime start = futureStart();
        ReservationRequest request = new ReservationRequest(
                "Soutenance", 999L, start, start.plusHours(2), 10, Set.of(), room.getId());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORGANIZER_NOT_FOUND"))
                .andExpect(jsonPath("$.details.organizerId").value(999));
    }

    @Test
    @DisplayName("Les reservations sont filtrables par salle et par periode")
    void shouldFilterReservations() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room first = givenRoom("Orion", building, 2, 30);
        Room second = givenRoom("Pegase", building, 3, 30);
        Organizer organizer = givenOrganizer("Alice Martin", "alice@example.org", building, 2);
        OffsetDateTime start = futureStart();
        reserve(organizer, first, start, start.plusHours(1)).andExpect(status().isCreated());
        reserve(organizer, second, start.plusHours(4), start.plusHours(5))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reservations").param("roomId", String.valueOf(first.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].room.id").value(first.getId()));

        mockMvc.perform(get("/api/reservations")
                        .param("from", start.plusHours(3).toString())
                        .param("to", start.plusHours(6).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].room.id").value(second.getId()));
    }

    @Test
    @DisplayName("Un intervalle de filtrage inverse renvoie INVALID_RESERVATION_PERIOD")
    void shouldRejectReversedFilterRange() throws Exception {
        OffsetDateTime start = futureStart();

        mockMvc.perform(get("/api/reservations")
                        .param("from", start.plusHours(2).toString())
                        .param("to", start.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_PERIOD"));
    }

    @Test
    @DisplayName("Consulter une reservation inexistante renvoie RESERVATION_NOT_FOUND")
    void shouldReturnNotFoundForUnknownReservation() throws Exception {
        mockMvc.perform(get("/api/reservations/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/reservations/42"))
                .andExpect(jsonPath("$.details.reservationId").value(42));
    }

    private ResultActions reserve(
            Organizer organizer, Room room, OffsetDateTime start, OffsetDateTime end) throws Exception {
        ReservationRequest request = new ReservationRequest(
                "Reunion", organizer.getId(), start, end, 10, Set.of(), room.getId());
        return mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));
    }
}
