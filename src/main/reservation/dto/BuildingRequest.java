package reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Corps des requetes de creation et de modification d'un batiment. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Min(1)
    @Max(200)
    private Integer numberOfFloors;
}
