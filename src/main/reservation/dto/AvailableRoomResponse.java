package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.Room;
import reservation.model.RoomStatus;

import java.util.List;

/** Salle proposee par la recherche de disponibilite, avec ses places inutilisees. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableRoomResponse {

    private Long id;
    private String name;
    private BuildingResponse building;
    private int floor;
    private int capacity;
    private RoomStatus status;
    private List<EquipmentResponse> equipment;
    private int unusedCapacity;

    public static AvailableRoomResponse from(Room room, int requestedCapacity) {
        return new AvailableRoomResponse(
                room.getId(),
                room.getName(),
                BuildingResponse.from(room.getBuilding()),
                room.getFloor(),
                room.getCapacity(),
                room.getStatus(),
                RoomResponse.equipmentOf(room),
                room.getCapacity() - requestedCapacity);
    }
}
