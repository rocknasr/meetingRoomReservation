package reservation.controller;

import reservation.dto.RoomRequest;
import reservation.model.Building;
import reservation.model.Equipment;
import reservation.model.Room;
import reservation.model.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration des endpoints de gestion et de recherche des salles.
 */
class RoomControllerTest extends AbstractControllerTest {

    @Test
    @DisplayName("Une salle creee est ensuite consultable")
    void shouldCreateAndReadRoom() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        givenEquipment("PROJECTOR", "Videoprojecteur");
        RoomRequest request = new RoomRequest(
                "Orion", building.getId(), 2, 30, Set.of("PROJECTOR"));

        String location = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Orion"))
                .andExpect(jsonPath("$.building.id").value(building.getId()))
                .andExpect(jsonPath("$.floor").value(2))
                .andExpect(jsonPath("$.capacity").value(30))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.equipment[0].code").value("PROJECTOR"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Orion"))
                .andExpect(jsonPath("$.equipment.length()").value(1));
    }

    @Test
    @DisplayName("Creer une salle dans un batiment inexistant renvoie BUILDING_NOT_FOUND")
    void shouldRejectRoomCreationWhenBuildingDoesNotExist() throws Exception {
        RoomRequest request = new RoomRequest("Orion", 999L, 0, 30, Set.of());

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BUILDING_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/rooms"))
                .andExpect(jsonPath("$.details.buildingId").value(999));
    }

    @Test
    @DisplayName("Un etage absent du batiment renvoie VALIDATION_ERROR")
    void shouldRejectRoomCreationWhenFloorIsOutOfBuilding() throws Exception {
        Building building = givenBuilding("Batiment A", 2);
        RoomRequest request = new RoomRequest("Orion", building.getId(), 2, 30, Set.of());

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.floor").exists());
    }

    @Test
    @DisplayName("Un nom de salle deja utilise renvoie RESOURCE_ALREADY_EXISTS")
    void shouldRejectDuplicateRoomName() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        givenRoom("Orion", building, 1, 20);
        RoomRequest request = new RoomRequest("orion", building.getId(), 2, 30, Set.of());

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("La recherche de disponibilite classe les salles par places inutilisees")
    void shouldListAvailableRoomsOrderedByUnusedCapacity() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        givenRoom("Grande", building, 1, 100);
        givenRoom("Moyenne", building, 1, 40);
        givenRoom("Juste", building, 1, 25);
        OffsetDateTime start = futureStart();

        mockMvc.perform(get("/api/rooms/available")
                        .param("start", start.toString())
                        .param("end", start.plusHours(2).toString())
                        .param("capacity", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Juste"))
                .andExpect(jsonPath("$[0].unusedCapacity").value(0))
                .andExpect(jsonPath("$[1].name").value("Moyenne"))
                .andExpect(jsonPath("$[1].unusedCapacity").value(15))
                .andExpect(jsonPath("$[2].name").value("Grande"));
    }

    @Test
    @DisplayName("La recherche de disponibilite ecarte les salles en maintenance et sans equipement")
    void shouldExcludeMaintenanceAndUnequippedRooms() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Equipment projector = givenEquipment("PROJECTOR", "Videoprojecteur");
        Room inMaintenance = givenRoom("Maintenance", building, 1, 30, projector);
        inMaintenance.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.saveAndFlush(inMaintenance);
        givenRoom("SansProjecteur", building, 1, 30);
        givenRoom("Conforme", building, 1, 30, projector);
        OffsetDateTime start = futureStart();

        mockMvc.perform(get("/api/rooms/available")
                        .param("start", start.toString())
                        .param("end", start.plusHours(2).toString())
                        .param("capacity", "10")
                        .param("equipment", "PROJECTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Conforme"));
    }

    @Test
    @DisplayName("Une periode inversee renvoie INVALID_RESERVATION_PERIOD")
    void shouldRejectAvailabilitySearchWithReversedPeriod() throws Exception {
        OffsetDateTime start = futureStart();

        mockMvc.perform(get("/api/rooms/available")
                        .param("start", start.toString())
                        .param("end", start.minusHours(1).toString())
                        .param("capacity", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_PERIOD"))
                .andExpect(jsonPath("$.path").value("/api/rooms/available"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    @DisplayName("Une capacite nulle renvoie VALIDATION_ERROR")
    void shouldRejectAvailabilitySearchWithInvalidCapacity() throws Exception {
        OffsetDateTime start = futureStart();

        mockMvc.perform(get("/api/rooms/available")
                        .param("start", start.toString())
                        .param("end", start.plusHours(1).toString())
                        .param("capacity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Consulter une salle inexistante renvoie ROOM_NOT_FOUND")
    void shouldReturnNotFoundForUnknownRoom() throws Exception {
        mockMvc.perform(get("/api/rooms/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.details.roomId").value(404));
    }

    @Test
    @DisplayName("Une salle peut etre placee en maintenance")
    void shouldPlaceRoomInMaintenance() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);

        mockMvc.perform(patch("/api/rooms/" + room.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    @DisplayName("Les equipements d'une salle sont entierement remplaces")
    void shouldReplaceRoomEquipment() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Equipment projector = givenEquipment("PROJECTOR", "Videoprojecteur");
        givenEquipment("WHITEBOARD", "Tableau blanc");
        Room room = givenRoom("Orion", building, 2, 30, projector);

        mockMvc.perform(put("/api/rooms/" + room.getId() + "/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentCodes\":[\"WHITEBOARD\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment.length()").value(1))
                .andExpect(jsonPath("$.equipment[0].code").value("WHITEBOARD"));
    }

    @Test
    @DisplayName("Un equipement inconnu renvoie EQUIPMENT_NOT_FOUND")
    void shouldRejectUnknownEquipmentOnRoom() throws Exception {
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);

        mockMvc.perform(put("/api/rooms/" + room.getId() + "/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentCodes\":[\"HOLOGRAM\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.details.equipmentCode").value("HOLOGRAM"));
    }
}
