package reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Corps de la requete de creation d'un equipement. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEquipmentRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$")
    private String code;

    @NotBlank
    @Size(max = 100)
    private String label;
}
