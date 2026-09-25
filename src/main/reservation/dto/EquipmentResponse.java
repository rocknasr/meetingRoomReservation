package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.Equipment;

/** Representation HTTP d'un equipement. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentResponse {

    private Long id;
    private String code;
    private String label;

    /** Construit la reponse a partir de l'equipement enregistre en base. */
    public static EquipmentResponse from(Equipment equipment) {
        return new EquipmentResponse(equipment.getId(), equipment.getCode(), equipment.getLabel());
    }
}
