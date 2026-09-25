package reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/** Corps de la requete de remplacement des equipements d'une salle. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplaceRoomEquipmentRequest {

    @NotNull
    private Set<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String> equipmentCodes;
}
