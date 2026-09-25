package reservation.controller;

import reservation.dto.CreateEquipmentRequest;
import reservation.dto.EquipmentResponse;
import reservation.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reservation.model.Equipment;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoints de gestion du catalogue d'equipements.
 */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(
            @Valid @RequestBody CreateEquipmentRequest request) {
        EquipmentResponse created = EquipmentResponse.from(equipmentService.create(request));
        return ResponseEntity.created(URI.create("/api/equipment/" + created.getId())).body(created);
    }

    @GetMapping
    public List<EquipmentResponse> listEquipment() {
        List<EquipmentResponse> response = new ArrayList<>();
        for (Equipment equipment : equipmentService.findAll()) {
            response.add(EquipmentResponse.from(equipment));
        }
        return response;
    }
}
