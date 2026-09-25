package reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/** Corps des requetes de creation et de modification d'une salle. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private Long buildingId;

    @NotNull
    @Min(0)
    @Max(199)
    private Integer floor;

    @NotNull
    @Min(1)
    private Integer capacity;

    private Set<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String> equipmentCodes;
}
