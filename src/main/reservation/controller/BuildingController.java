package reservation.controller;

import reservation.dto.BuildingRequest;
import reservation.dto.BuildingResponse;
import reservation.service.BuildingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reservation.model.Building;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoints de gestion des batiments.
 */
@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @PostMapping
    public ResponseEntity<BuildingResponse> createBuilding(@Valid @RequestBody BuildingRequest request) {
        BuildingResponse created = BuildingResponse.from(buildingService.create(request));
        return ResponseEntity.created(URI.create("/api/buildings/" + created.getId())).body(created);
    }

    @GetMapping
    public List<BuildingResponse> listBuildings() {
        List<BuildingResponse> response = new ArrayList<>();
        for (Building building : buildingService.findAll()) {
            response.add(BuildingResponse.from(building));
        }
        return response;
    }

    @GetMapping("/{buildingId}")
    public BuildingResponse getBuilding(@PathVariable Long buildingId) {
        return BuildingResponse.from(buildingService.findById(buildingId));
    }

    @PutMapping("/{buildingId}")
    public BuildingResponse updateBuilding(
            @PathVariable Long buildingId, @Valid @RequestBody BuildingRequest request) {
        return BuildingResponse.from(buildingService.update(buildingId, request));
    }
}
