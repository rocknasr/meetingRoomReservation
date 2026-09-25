package reservation.controller;

import reservation.dto.AvailableRoomResponse;
import reservation.dto.ReplaceRoomEquipmentRequest;
import reservation.dto.RoomRequest;
import reservation.dto.RoomResponse;
import reservation.dto.UpdateRoomStatusRequest;
import reservation.model.Room;
import reservation.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Endpoints de gestion et de recherche des salles.
 */
@Validated
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        RoomResponse created = RoomResponse.from(roomService.create(request));
        return ResponseEntity.created(URI.create("/api/rooms/" + created.getId())).body(created);
    }

    @GetMapping
    public List<RoomResponse> listRooms() {
        List<RoomResponse> response = new ArrayList<>();
        for (Room room : roomService.findAll()) {
            response.add(RoomResponse.from(room));
        }
        return response;
    }

    @GetMapping("/available")
    public List<AvailableRoomResponse> findAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam @Min(1) int capacity,
            @RequestParam(required = false) Set<String> equipment) {
        // ?equipment=PROJECTOR,WHITEBOARD est converti en ensemble par Spring.
        Set<String> requiredEquipment = equipment == null ? Set.of() : new LinkedHashSet<>(equipment);
        List<Room> rooms = roomService.findAvailable(
                start.toInstant(), end.toInstant(), capacity, requiredEquipment);

        List<AvailableRoomResponse> response = new ArrayList<>();
        for (Room room : rooms) {
            response.add(AvailableRoomResponse.from(room, capacity));
        }
        return response;
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoom(@PathVariable Long roomId) {
        return RoomResponse.from(roomService.findById(roomId));
    }

    @PutMapping("/{roomId}")
    public RoomResponse updateRoom(
            @PathVariable Long roomId, @Valid @RequestBody RoomRequest request) {
        return RoomResponse.from(roomService.update(roomId, request));
    }

    @PatchMapping("/{roomId}/status")
    public RoomResponse updateRoomStatus(
            @PathVariable Long roomId, @Valid @RequestBody UpdateRoomStatusRequest request) {
        return RoomResponse.from(roomService.updateStatus(roomId, request.getStatus()));
    }

    @PutMapping("/{roomId}/equipment")
    public RoomResponse replaceRoomEquipment(
            @PathVariable Long roomId, @Valid @RequestBody ReplaceRoomEquipmentRequest request) {
        return RoomResponse.from(roomService.replaceEquipment(roomId, request));
    }
}
