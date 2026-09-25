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

    /**
     * Cree un equipement dont le code doit etre unique.
     *
     * @param request code et libelle de l'equipement
     * @return l'equipement cree
     * @throws ResourceAlreadyExistsException si le code est deja utilise
     */
    public Equipment create(CreateEquipmentRequest request) {
        if (equipmentRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw ResourceAlreadyExistsException.equipment();
        }
        return equipmentRepository.save(new Equipment(request.getCode(), request.getLabel()));
    }

    /**
     * @return tous les equipements, tries par code croissant
     */
    public List<Equipment> findAll() {
        List<Equipment> equipment = new ArrayList<>(equipmentRepository.findAll());
        equipment.sort(Comparator.comparing(Equipment::getCode));
        return equipment;
    }

    /**
     * Resout des codes d'equipement en equipements existants.
     *
     * @param codes codes a resoudre, eventuellement vides
     * @return les equipements correspondants, dans l'ordre des codes
     * @throws ResourceNotFoundException des qu'un code ne correspond a aucun equipement
     */
    public Set<Equipment> resolveAll(Set<String> codes) {
        Set<Equipment> resolved = new LinkedHashSet<>();
        for (String code : codes) {
            resolved.add(equipmentRepository.findByCode(code)
                    .orElseThrow(() -> ResourceNotFoundException.equipment(code)));
        }
        return resolved;
    }

    /**
     * Verifie que tous les codes designent des equipements existants, sans charger
     * les entites dans la reponse.
     *
     * @param codes codes a verifier
     * @throws ResourceNotFoundException des qu'un code est inconnu
     */
    public void checkAllExist(Set<String> codes) {
        resolveAll(codes);
    }
}
