package reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Corps de la requete de creation d'un organisateur. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizerRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotNull
    private Long buildingId;

    @NotNull
    @Min(0)
    @Max(199)
    private Integer floor;
}
