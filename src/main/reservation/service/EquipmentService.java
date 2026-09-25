package reservation.service;

import reservation.dto.CreateEquipmentRequest;
import reservation.exception.ResourceAlreadyExistsException;
import reservation.exception.ResourceNotFoundException;
import reservation.model.Equipment;
import reservation.repository.EquipmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestion du catalogue d'equipements.
 */
@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public Equipment create(CreateEquipmentRequest request) {
        if (equipmentRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw ResourceAlreadyExistsException.equipment();
        }
        return equipmentRepository.save(new Equipment(request.getCode(), request.getLabel()));
    }

    public List<Equipment> findAll() {
        List<Equipment> equipment = new ArrayList<>(equipmentRepository.findAll());
        equipment.sort(Comparator.comparing(Equipment::getCode));
        return equipment;
    }

    public Set<Equipment> resolveAll(Set<String> codes) {
        Set<Equipment> resolved = new LinkedHashSet<>();
        for (String code : codes) {
            resolved.add(equipmentRepository.findByCode(code)
                    .orElseThrow(() -> ResourceNotFoundException.equipment(code)));
        }
        return resolved;
    }

    public void checkAllExist(Set<String> codes) {
        resolveAll(codes);
    }
}
