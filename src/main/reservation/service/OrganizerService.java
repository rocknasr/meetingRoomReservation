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

    /**
     * Cree un organisateur localise dans un batiment existant.
     *
     * @param request identite, adresse e-mail et localisation
     * @return l'organisateur cree
     * @throws ResourceNotFoundException      si le batiment n'existe pas
     * @throws ValidationException            si l'etage n'existe pas dans ce batiment
     * @throws ResourceAlreadyExistsException si l'adresse e-mail est deja utilisee
     */
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

    /**
     * @return tous les organisateurs, tries par nom puis par identifiant
     */
    public List<Organizer> findAll() {
        List<Organizer> organizers = new ArrayList<>(organizerRepository.findAll());
        organizers.sort(Comparator
                .comparing(Organizer::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Organizer::getId));
        return organizers;
    }

    /**
     * @param id identifiant recherche
     * @return l'organisateur correspondant
     * @throws ResourceNotFoundException si l'organisateur n'existe pas
     */
    public Organizer findById(Long id) {
        return organizerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.organizer(id));
    }
}
