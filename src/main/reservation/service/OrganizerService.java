package reservation.service;

import reservation.dto.CreateOrganizerRequest;
import reservation.exception.ResourceAlreadyExistsException;
import reservation.exception.ResourceNotFoundException;
import reservation.exception.ValidationException;
import reservation.model.Building;
import reservation.model.Organizer;
import reservation.repository.OrganizerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Gestion des organisateurs et de leur localisation.
 */
@Service
public class OrganizerService {

    private final OrganizerRepository organizerRepository;
    private final BuildingService buildingService;

    public OrganizerService(OrganizerRepository organizerRepository, BuildingService buildingService) {
        this.organizerRepository = organizerRepository;
        this.buildingService = buildingService;
    }

    public Organizer create(CreateOrganizerRequest request) {
        Building building = buildingService.findById(request.getBuildingId());
        if (!building.hasFloor(request.getFloor())) {
            throw ValidationException.floorOutOfBuilding("floor", building.getNumberOfFloors());
        }
        if (organizerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw ResourceAlreadyExistsException.organizer();
        }
        return organizerRepository.save(
                new Organizer(request.getName(), request.getEmail(), building, request.getFloor()));
    }

    public List<Organizer> findAll() {
        List<Organizer> organizers = new ArrayList<>(organizerRepository.findAll());
        organizers.sort(Comparator
                .comparing(Organizer::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Organizer::getId));
        return organizers;
    }

    public Organizer findById(Long id) {
        return organizerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.organizer(id));
    }
}
