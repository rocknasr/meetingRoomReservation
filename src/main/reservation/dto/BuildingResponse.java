package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.Building;

/** Representation HTTP d'un batiment. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingResponse {

    private Long id;
    private String name;
    private int numberOfFloors;

    public static BuildingResponse from(Building building) {
        return new BuildingResponse(building.getId(), building.getName(), building.getNumberOfFloors());
    }
}
