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
        // GIVEN un batiment de cinq etages et un videoprojecteur au catalogue
        Building building = givenBuilding("Batiment A", 5);
        givenEquipment("PROJECTOR", "Videoprojecteur");
        RoomRequest request = new RoomRequest(
                "Orion", building.getId(), 2, 30, Set.of("PROJECTOR"));

        // WHEN on cree la salle
        String location = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                // THEN la reponse est 201 et decrit la salle creee
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

        // AND la salle est consultable a l'URI renvoyee
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Orion"))
                .andExpect(jsonPath("$.equipment.length()").value(1));
    }

    @Test
    @DisplayName("Creer une salle dans un batiment inexistant renvoie BUILDING_NOT_FOUND")
    void shouldRejectRoomCreationWhenBuildingDoesNotExist() throws Exception {
        // GIVEN une demande referencant un batiment absent
        RoomRequest request = new RoomRequest("Orion", 999L, 0, 30, Set.of());

        // WHEN on cree la salle
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                // THEN la reponse suit le format d'erreur du contrat
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
        // GIVEN un batiment de deux etages, numerotes 0 et 1
        Building building = givenBuilding("Batiment A", 2);
        RoomRequest request = new RoomRequest("Orion", building.getId(), 2, 30, Set.of());

        // WHEN on cree une salle au 2e etage
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                // THEN la reponse nomme le champ fautif
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.floor").exists());
    }

    @Test
    @DisplayName("Un nom de salle deja utilise renvoie RESOURCE_ALREADY_EXISTS")
    void shouldRejectDuplicateRoomName() throws Exception {
        // GIVEN une salle nommee Orion
        Building building = givenBuilding("Batiment A", 5);
        givenRoom("Orion", building, 1, 20);
        RoomRequest request = new RoomRequest("orion", building.getId(), 2, 30, Set.of());

        // WHEN on cree une salle portant le meme nom a la casse pres
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                // THEN la demande est refusee
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("La recherche de disponibilite classe les salles par places inutilisees")
    void shouldListAvailableRoomsOrderedByUnusedCapacity() throws Exception {
        // GIVEN trois salles de capacites differentes
        Building building = givenBuilding("Batiment A", 5);
        givenRoom("Grande", building, 1, 100);
        givenRoom("Moyenne", building, 1, 40);
        givenRoom("Juste", building, 1, 25);
        OffsetDateTime start = futureStart();

        // WHEN on cherche une salle pour 25 participants
        mockMvc.perform(get("/api/rooms/available")
                        .param("start", start.toString())
                        .param("end", start.plusHours(2).toString())
                        .param("capacity", "25"))
                // THEN les salles les plus justes viennent en premier
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
        // GIVEN une salle en maintenance, une salle sans videoprojecteur et une salle conforme
        Building building = givenBuilding("Batiment A", 5);
        Equipment projector = givenEquipment("PROJECTOR", "Videoprojecteur");
        Room inMaintenance = givenRoom("Maintenance", building, 1, 30, projector);
        inMaintenance.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.saveAndFlush(inMaintenance);
        givenRoom("SansProjecteur", building, 1, 30);
        givenRoom("Conforme", building, 1, 30, projector);
        OffsetDateTime start = futureStart();

        // WHEN on cherche une salle equipee d'un videoprojecteur
        mockMvc.perform(get("/api/rooms/available")
                        .param("start", start.toString())
                        .param("end", start.plusHours(2).toString())
                        .param("capacity", "10")
                        .param("equipment", "PROJECTOR"))
                // THEN seule la salle conforme est proposee
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Conforme"));
    }

    @Test
    @DisplayName("Une periode inversee renvoie INVALID_RESERVATION_PERIOD")
    void shouldRejectAvailabilitySearchWithReversedPeriod() throws Exception {
        // GIVEN une periode dont la fin precede le debut
        OffsetDateTime start = futureStart();

        // WHEN on recherche les salles disponibles
        mockMvc.perform(get("/api/rooms/available")
                        .param("start", start.toString())
                        .param("end", start.minusHours(1).toString())
                        .param("capacity", "10"))
                // THEN la reponse suit le format d'erreur du contrat
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_PERIOD"))
                .andExpect(jsonPath("$.path").value("/api/rooms/available"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    @DisplayName("Une capacite nulle renvoie VALIDATION_ERROR")
    void shouldRejectAvailabilitySearchWithInvalidCapacity() throws Exception {
        // GIVEN une recherche demandant zero participant
        OffsetDateTime start = futureStart();

        // WHEN on recherche les salles disponibles
        mockMvc.perform(get("/api/rooms/available")
                        .param("start", start.toString())
                        .param("end", start.plusHours(1).toString())
                        .param("capacity", "0"))
                // THEN la demande est refusee
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Consulter une salle inexistante renvoie ROOM_NOT_FOUND")
    void shouldReturnNotFoundForUnknownRoom() throws Exception {
        // GIVEN aucune salle enregistree
        // WHEN on consulte la salle 404
        mockMvc.perform(get("/api/rooms/404"))
                // THEN la reponse suit le format d'erreur du contrat
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.details.roomId").value(404));
    }

    @Test
    @DisplayName("Une salle peut etre placee en maintenance")
    void shouldPlaceRoomInMaintenance() throws Exception {
        // GIVEN une salle disponible
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);

        // WHEN on la place en maintenance
        mockMvc.perform(patch("/api/rooms/" + room.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MAINTENANCE\"}"))
                // THEN son statut est mis a jour
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    @DisplayName("Les equipements d'une salle sont entierement remplaces")
    void shouldReplaceRoomEquipment() throws Exception {
        // GIVEN une salle equipee d'un videoprojecteur
        Building building = givenBuilding("Batiment A", 5);
        Equipment projector = givenEquipment("PROJECTOR", "Videoprojecteur");
        givenEquipment("WHITEBOARD", "Tableau blanc");
        Room room = givenRoom("Orion", building, 2, 30, projector);

        // WHEN on remplace ses equipements par un tableau blanc
        mockMvc.perform(put("/api/rooms/" + room.getId() + "/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentCodes\":[\"WHITEBOARD\"]}"))
                // THEN seule la nouvelle liste subsiste
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment.length()").value(1))
                .andExpect(jsonPath("$.equipment[0].code").value("WHITEBOARD"));
    }

    @Test
    @DisplayName("Un equipement inconnu renvoie EQUIPMENT_NOT_FOUND")
    void shouldRejectUnknownEquipmentOnRoom() throws Exception {
        // GIVEN une salle existante
        Building building = givenBuilding("Batiment A", 5);
        Room room = givenRoom("Orion", building, 2, 30);

        // WHEN on lui installe un equipement absent du catalogue
        mockMvc.perform(put("/api/rooms/" + room.getId() + "/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentCodes\":[\"HOLOGRAM\"]}"))
                // THEN la demande est refusee
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.details.equipmentCode").value("HOLOGRAM"));
    }
}
