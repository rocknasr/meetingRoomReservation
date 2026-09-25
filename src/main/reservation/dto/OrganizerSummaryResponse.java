package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.Organizer;

/** Organisateur tel qu'il apparait dans une reservation, sans son adresse e-mail. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerSummaryResponse {

    private Long id;
    private String name;
    private BuildingResponse building;
    private int floor;

    public static OrganizerSummaryResponse from(Organizer organizer) {
        return new OrganizerSummaryResponse(
                organizer.getId(),
                organizer.getName(),
                BuildingResponse.from(organizer.getBuilding()),
                organizer.getFloor());
    }
}
