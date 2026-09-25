package reservation.controller;

import tools.jackson.databind.ObjectMapper;
import reservation.model.Building;
import reservation.model.Equipment;
import reservation.model.Organizer;
import reservation.model.Room;
import reservation.repository.BuildingRepository;
import reservation.repository.EquipmentRepository;
import reservation.repository.OrganizerRepository;
import reservation.repository.ReservationRepository;
import reservation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * Socle commun aux tests d'integration : contexte Spring complet, MockMvc et jeu
 * de donnees reconstruit avant chaque test.
 *
 * <p>La base est videe avant chaque test, de sorte que les donnees inserees ici
 * n'influencent pas les tests suivants.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected BuildingRepository buildingRepository;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected EquipmentRepository equipmentRepository;

    @Autowired
    protected OrganizerRepository organizerRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    /**
     * Vide la base des donnees d'exemple livrees par la migration, afin que chaque
     * test parte d'un etat connu.
     */
    @BeforeEach
    void clearDatabase() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        organizerRepository.deleteAll();
        equipmentRepository.deleteAll();
        buildingRepository.deleteAll();
        flush();
    }

    /**
     * Force l'ecriture des modifications en attente avant l'appel HTTP suivant.
     */
    protected void flush() {
        reservationRepository.flush();
        roomRepository.flush();
        organizerRepository.flush();
        equipmentRepository.flush();
        buildingRepository.flush();
    }

    /**
     * @param name           nom du batiment
     * @param numberOfFloors nombre d'etages
     * @return le batiment persiste
     */
    protected Building givenBuilding(String name, int numberOfFloors) {
        return buildingRepository.saveAndFlush(new Building(name, numberOfFloors));
    }

    /**
     * @param code  code de l'equipement
     * @param label libelle de l'equipement
     * @return l'equipement persiste
     */
    protected Equipment givenEquipment(String code, String label) {
        return equipmentRepository.saveAndFlush(new Equipment(code, label));
    }

    /**
     * @param name      nom de la salle
     * @param building  batiment d'accueil
     * @param floor     etage
     * @param capacity  capacite
     * @param equipment equipements installes
     * @return la salle persistee
     */
    protected Room givenRoom(String name, Building building, int floor, int capacity, Equipment... equipment) {
        Room room = new Room(name, building, floor, capacity);
        room.setEquipment(new LinkedHashSet<>(Arrays.asList(equipment)));
        return roomRepository.saveAndFlush(room);
    }

    /**
     * @param name     nom de l'organisateur
     * @param email    adresse e-mail
     * @param building batiment de rattachement
     * @param floor    etage de rattachement
     * @return l'organisateur persiste
     */
    protected Organizer givenOrganizer(String name, String email, Building building, int floor) {
        return organizerRepository.saveAndFlush(new Organizer(name, email, building, floor));
    }

    /**
     * Fournit un debut de periode toujours situe dans le futur, a l'heure ronde.
     *
     * @return le debut de periode utilise par les tests
     */
    protected OffsetDateTime futureStart() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).truncatedTo(ChronoUnit.HOURS);
    }

    /**
     * @param body objet a envoyer
     * @return sa representation JSON
     */
    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
