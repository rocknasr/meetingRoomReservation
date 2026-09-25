package reservation.controller;

import reservation.dto.CreateOrganizerRequest;
import reservation.dto.OrganizerResponse;
import reservation.service.OrganizerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reservation.model.Organizer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoints de gestion des organisateurs.
 */
@RestController
@RequestMapping("/api/organizers")
public class OrganizerController {

    private final OrganizerService organizerService;

    public OrganizerController(OrganizerService organizerService) {
        this.organizerService = organizerService;
    }

    /**
     * Cree un organisateur localise dans un batiment existant.
     *
     * @param request identite, adresse e-mail et localisation
     * @return 201 avec l'organisateur cree et son URI
     */
    @PostMapping
    public ResponseEntity<OrganizerResponse> createOrganizer(
            @Valid @RequestBody CreateOrganizerRequest request) {
        OrganizerResponse created = OrganizerResponse.from(organizerService.create(request));
        return ResponseEntity.created(URI.create("/api/organizers/" + created.getId())).body(created);
    }

    /**
     * @return tous les organisateurs, tries par nom puis par identifiant
     */
    @GetMapping
    public List<OrganizerResponse> listOrganizers() {
        List<OrganizerResponse> response = new ArrayList<>();
        for (Organizer organizer : organizerService.findAll()) {
            response.add(OrganizerResponse.from(organizer));
        }
        return response;
    }

    /**
     * @param organizerId identifiant de l'organisateur
     * @return l'organisateur demande
     */
    @GetMapping("/{organizerId}")
    public OrganizerResponse getOrganizer(@PathVariable Long organizerId) {
        return OrganizerResponse.from(organizerService.findById(organizerId));
    }
}
