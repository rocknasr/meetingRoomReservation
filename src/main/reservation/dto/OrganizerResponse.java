package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.Organizer;

/** Organisateur complet, adresse e-mail comprise. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerResponse {

    private Long id;
    private String name;
    private BuildingResponse building;
    private int floor;
    private String email;

    public static OrganizerResponse from(Organizer organizer) {
        return new OrganizerResponse(
                organizer.getId(),
                organizer.getName(),
                BuildingResponse.from(organizer.getBuilding()),
                organizer.getFloor(),
                organizer.getEmail());
    }
}
