package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.Room;
import reservation.model.RoomStatus;

/** Salle telle qu'elle apparait dans une reservation, sans ses equipements. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomSummaryResponse {

    private Long id;
    private String name;
    private BuildingResponse building;
    private int floor;
    private int capacity;
    private RoomStatus status;

    /** Construit la reponse a partir de la salle enregistree en base. */
    public static RoomSummaryResponse from(Room room) {
        return new RoomSummaryResponse(
                room.getId(),
                room.getName(),
                BuildingResponse.from(room.getBuilding()),
                room.getFloor(),
                room.getCapacity(),
                room.getStatus());
    }
}
