package reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Corps des deux formes de demande de reservation. Le roomId est obligatoire pour
 * reserver une salle precise, et inutilise en attribution automatique.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private Long organizerId;

    @NotNull
    private OffsetDateTime start;

    @NotNull
    private OffsetDateTime end;

    @NotNull
    @Min(1)
    private Integer numberOfParticipants;

    private Set<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String> requiredEquipmentCodes;

    private Long roomId;
}
