package reservation.service;

import reservation.dto.BuildingRequest;
import reservation.exception.ConflictException;
import reservation.exception.ResourceAlreadyExistsException;
import reservation.exception.ResourceNotFoundException;
import reservation.model.Building;
import reservation.model.Organizer;
import reservation.model.Room;
import reservation.repository.BuildingRepository;
import reservation.repository.OrganizerRepository;
import reservation.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Gestion des batiments et de leur nombre d'etages.
 */
@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final OrganizerRepository organizerRepository;

    public BuildingService(
            BuildingRepository buildingRepository,
            RoomRepository roomRepository,
            OrganizerRepository organizerRepository) {
        this.buildingRepository = buildingRepository;
        this.roomRepository = roomRepository;
        this.organizerRepository = organizerRepository;
    }

    public Building create(BuildingRequest request) {
        if (buildingRepository.existsByNameIgnoreCase(request.getName())) {
            throw ResourceAlreadyExistsException.building();
        }
        return buildingRepository.save(new Building(request.getName(), request.getNumberOfFloors()));
    }

    public List<Building> findAll() {
        List<Building> buildings = new ArrayList<>(buildingRepository.findAll());
        buildings.sort(Comparator.comparing(Building::getName, String.CASE_INSENSITIVE_ORDER));
        return buildings;
    }

    public Building findById(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.building(id));
    }

    public Building update(Long id, BuildingRequest request) {
        Building building = findById(id);

        if (buildingRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw ResourceAlreadyExistsException.building();
        }

        // On ne peut pas supprimer un etage encore occupe par une salle ou un organisateur.
        int highestOccupiedFloor = findHighestOccupiedFloor(id);
        if (highestOccupiedFloor >= request.getNumberOfFloors()) {
            throw ConflictException.buildingFloorCount(highestOccupiedFloor);
        }

        building.setName(request.getName());
        building.setNumberOfFloors(request.getNumberOfFloors());
        return buildingRepository.save(building);
    }

    private int findHighestOccupiedFloor(Long buildingId) {
        // -1 signifie qu'aucun etage n'est occupe.
        int highest = -1;
        for (Room room : roomRepository.findByBuildingId(buildingId)) {
            highest = Math.max(highest, room.getFloor());
        }
        for (Organizer organizer : organizerRepository.findByBuildingId(buildingId)) {
            highest = Math.max(highest, organizer.getFloor());
        }
        return highest;
    }
}
