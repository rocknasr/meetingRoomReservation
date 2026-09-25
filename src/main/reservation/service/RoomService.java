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

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>(roomRepository.findAll());
        rooms.sort(Comparator.comparing(Room::getName, String.CASE_INSENSITIVE_ORDER));
        return rooms;
    }

    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.room(id));
    }

    public Room update(Long id, RoomRequest request) {
        Room room = findById(id);
        Building building = buildingService.findById(request.getBuildingId());
        if (!building.hasFloor(request.getFloor())) {
            throw ValidationException.floorOutOfBuilding("floor", building.getNumberOfFloors());
        }
        if (roomRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw ResourceAlreadyExistsException.room();
        }

        // Le statut et les equipements se modifient par leurs propres endpoints.
        room.setName(request.getName());
        room.setBuilding(building);
        room.setFloor(request.getFloor());
        room.setCapacity(request.getCapacity());
        return roomRepository.save(room);
    }

    public Room updateStatus(Long id, RoomStatus status) {
        Room room = findById(id);
        room.setStatus(status);
        return roomRepository.save(room);
    }

    public Room replaceEquipment(Long id, ReplaceRoomEquipmentRequest request) {
        Room room = findById(id);
        room.setEquipment(new LinkedHashSet<>(equipmentService.resolveAll(request.getEquipmentCodes())));
        return roomRepository.save(room);
    }

    public List<Room> findAvailable(Instant start, Instant end, int capacity, Set<String> equipmentCodes) {
        // Simple consultation : seule la chronologie est verifiee, pas la duree ni le passe.
        periodValidator.validateChronology(start, end);

        List<Reservation> confirmed = reservationRepository.findByStatus(ReservationStatus.CONFIRMED);
        List<Room> compatible = roomAssignmentService.findCompatibleRooms(
                roomRepository.findAll(), capacity, equipmentCodes, confirmed, start, end);
        return roomAssignmentService.sortByUnusedCapacity(compatible, capacity);
    }
}
