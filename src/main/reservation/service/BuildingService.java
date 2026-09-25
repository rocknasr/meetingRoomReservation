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

    /**
     * Cree un batiment au nom unique.
     *
     * @param request nom et nombre d'etages
     * @return le batiment cree
     * @throws ResourceAlreadyExistsException si le nom est deja utilise
     */
    public Building create(BuildingRequest request) {
        if (buildingRepository.existsByNameIgnoreCase(request.getName())) {
            throw ResourceAlreadyExistsException.building();
        }
        return buildingRepository.save(new Building(request.getName(), request.getNumberOfFloors()));
    }

    /**
     * @return tous les batiments, tries par nom sans tenir compte de la casse
     */
    public List<Building> findAll() {
        List<Building> buildings = new ArrayList<>(buildingRepository.findAll());
        buildings.sort(Comparator.comparing(Building::getName, String.CASE_INSENSITIVE_ORDER));
        return buildings;
    }

    /**
     * @param id identifiant recherche
     * @return le batiment correspondant
     * @throws ResourceNotFoundException si le batiment n'existe pas
     */
    public Building findById(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.building(id));
    }

    /**
     * Remplace le nom et le nombre d'etages d'un batiment.
     *
     * <p>La reduction du nombre d'etages est refusee lorsqu'une salle ou un
     * organisateur occupe un etage qui deviendrait invalide.</p>
     *
     * @param id      identifiant du batiment
     * @param request nouveau nom et nouveau nombre d'etages
     * @return le batiment mis a jour
     * @throws ResourceNotFoundException      si le batiment n'existe pas
     * @throws ResourceAlreadyExistsException si un autre batiment porte ce nom
     * @throws ConflictException              si un etage occupe disparaitrait
     */
    public Building update(Long id, BuildingRequest request) {
        Building building = findById(id);

        if (buildingRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw ResourceAlreadyExistsException.building();
        }

        int highestOccupiedFloor = findHighestOccupiedFloor(id);
        if (highestOccupiedFloor >= request.getNumberOfFloors()) {
            throw ConflictException.buildingFloorCount(highestOccupiedFloor);
        }

        building.setName(request.getName());
        building.setNumberOfFloors(request.getNumberOfFloors());
        return buildingRepository.save(building);
    }

    /**
     * Determine l'etage occupe le plus eleve du batiment, salles et organisateurs
     * confondus.
     *
     * @param buildingId identifiant du batiment
     * @return l'etage le plus eleve, ou -1 lorsque le batiment est vide
     */
    private int findHighestOccupiedFloor(Long buildingId) {
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
