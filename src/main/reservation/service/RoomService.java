package reservation.service;

import reservation.dto.ReplaceRoomEquipmentRequest;
import reservation.dto.RoomRequest;
import reservation.exception.ResourceAlreadyExistsException;
import reservation.exception.ResourceNotFoundException;
import reservation.exception.ValidationException;
import reservation.model.Building;
import reservation.model.Reservation;
import reservation.model.ReservationStatus;
import reservation.model.Room;
import reservation.model.RoomStatus;
import reservation.repository.ReservationRepository;
import reservation.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestion des salles : creation, mise a jour, equipements, statut et recherche de
 * disponibilite.
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final BuildingService buildingService;
    private final EquipmentService equipmentService;
    private final RoomAssignmentService roomAssignmentService;
    private final ReservationPeriodValidator periodValidator;

    public RoomService(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            BuildingService buildingService,
            EquipmentService equipmentService,
            RoomAssignmentService roomAssignmentService,
            ReservationPeriodValidator periodValidator) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.buildingService = buildingService;
        this.equipmentService = equipmentService;
        this.roomAssignmentService = roomAssignmentService;
        this.periodValidator = periodValidator;
    }

    /**
     * Cree une salle disponible dans un batiment existant.
     *
     * @param request description de la salle et de ses equipements
     * @return la salle creee
     * @throws ResourceNotFoundException      si le batiment ou un equipement n'existe pas
     * @throws ValidationException            si l'etage n'existe pas dans ce batiment
     * @throws ResourceAlreadyExistsException si le nom est deja utilise
     */
    public Room create(RoomRequest request) {
        Building building = buildingService.findById(request.getBuildingId());
        if (!building.hasFloor(request.getFloor())) {
            throw ValidationException.floorOutOfBuilding("floor", building.getNumberOfFloors());
        }
        if (roomRepository.existsByNameIgnoreCase(request.getName())) {
            throw ResourceAlreadyExistsException.room();
        }

        Room room = new Room(request.getName(), building, request.getFloor(), request.getCapacity());
        room.setStatus(RoomStatus.AVAILABLE);
        Set<String> equipmentCodes =
                request.getEquipmentCodes() == null ? Set.of() : request.getEquipmentCodes();
        room.setEquipment(new LinkedHashSet<>(equipmentService.resolveAll(equipmentCodes)));
        return roomRepository.save(room);
    }

    /**
     * @return toutes les salles, triees par nom sans tenir compte de la casse
     */
    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>(roomRepository.findAll());
        rooms.sort(Comparator.comparing(Room::getName, String.CASE_INSENSITIVE_ORDER));
        return rooms;
    }

    /**
     * @param id identifiant recherche
     * @return la salle correspondante
     * @throws ResourceNotFoundException si la salle n'existe pas
     */
    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.room(id));
    }

    /**
     * Remplace le nom, le batiment, l'etage et la capacite d'une salle. Le statut
     * et les equipements sont conserves.
     *
     * @param id      identifiant de la salle
     * @param request nouvelles informations
     * @return la salle mise a jour
     * @throws ResourceNotFoundException      si la salle ou le batiment n'existe pas
     * @throws ValidationException            si l'etage n'existe pas dans ce batiment
     * @throws ResourceAlreadyExistsException si une autre salle porte ce nom
     */
    public Room update(Long id, RoomRequest request) {
        Room room = findById(id);
        Building building = buildingService.findById(request.getBuildingId());
        if (!building.hasFloor(request.getFloor())) {
            throw ValidationException.floorOutOfBuilding("floor", building.getNumberOfFloors());
        }
        if (roomRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw ResourceAlreadyExistsException.room();
        }

        room.setName(request.getName());
        room.setBuilding(building);
        room.setFloor(request.getFloor());
        room.setCapacity(request.getCapacity());
        // Le statut et les equipements ne sont volontairement pas modifies ici.
        return roomRepository.save(room);
    }

    /**
     * Place la salle en maintenance ou la remet a disposition. Les reservations
     * existantes sont conservees.
     *
     * @param id     identifiant de la salle
     * @param status nouvel etat
     * @return la salle mise a jour
     * @throws ResourceNotFoundException si la salle n'existe pas
     */
    public Room updateStatus(Long id, RoomStatus status) {
        Room room = findById(id);
        room.setStatus(status);
        return roomRepository.save(room);
    }

    /**
     * Remplace entierement les equipements d'une salle.
     *
     * @param id      identifiant de la salle
     * @param request codes des equipements a installer
     * @return la salle mise a jour
     * @throws ResourceNotFoundException si la salle ou un equipement n'existe pas
     */
    public Room replaceEquipment(Long id, ReplaceRoomEquipmentRequest request) {
        Room room = findById(id);
        room.setEquipment(new LinkedHashSet<>(equipmentService.resolveAll(request.getEquipmentCodes())));
        return roomRepository.save(room);
    }

    /**
     * Recherche les salles disponibles et compatibles sur une periode.
     *
     * <p>Aucune reservation n'est creee : la methode renvoie simplement les salles
     * retenues, classees par places inutilisees croissantes, puis par nom, puis par
     * identifiant.</p>
     *
     * @param start          debut de la periode
     * @param end            fin de la periode
     * @param capacity       nombre de participants a accueillir
     * @param equipmentCodes codes des equipements exiges, eventuellement vides
     * @return les salles compatibles, eventuellement aucune
     */
    public List<Room> findAvailable(Instant start, Instant end, int capacity, Set<String> equipmentCodes) {
        periodValidator.validateChronology(start, end);

        // Les reservations annulees sont ignorees : elles ne bloquent plus la salle.
        List<Reservation> confirmed = reservationRepository.findByStatus(ReservationStatus.CONFIRMED);
        List<Room> compatible = roomAssignmentService.findCompatibleRooms(
                roomRepository.findAll(), capacity, equipmentCodes, confirmed, start, end);
        return roomAssignmentService.sortByUnusedCapacity(compatible, capacity);
    }
}
