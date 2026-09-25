package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.Equipment;
import reservation.model.Room;
import reservation.model.RoomStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Salle complete, equipements compris. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long id;
    private String name;
    private BuildingResponse building;
    private int floor;
    private int capacity;
    private RoomStatus status;
    private List<EquipmentResponse> equipment;

    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                BuildingResponse.from(room.getBuilding()),
                room.getFloor(),
                room.getCapacity(),
                room.getStatus(),
                equipmentOf(room));
    }

    static List<EquipmentResponse> equipmentOf(Room room) {
        List<Equipment> equipment = new ArrayList<>(room.getEquipment());
        equipment.sort(Comparator.comparing(Equipment::getCode));

        List<EquipmentResponse> response = new ArrayList<>();
        for (Equipment item : equipment) {
            response.add(EquipmentResponse.from(item));
        }
        return response;
    }
}
