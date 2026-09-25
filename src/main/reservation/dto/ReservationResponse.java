package reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.Reservation;
import reservation.model.ReservationStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Representation HTTP d'une reservation, confirmee ou annulee. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private String title;
    private ReservationStatus status;
    private RoomSummaryResponse room;
    private OrganizerSummaryResponse organizer;
    private OffsetDateTime start;
    private OffsetDateTime end;
    private int numberOfParticipants;
    private List<String> requiredEquipmentCodes;
    private OffsetDateTime createdAt;

    public static ReservationResponse from(Reservation reservation) {
        List<String> equipmentCodes = new ArrayList<>(reservation.getRequiredEquipmentCodes());
        Collections.sort(equipmentCodes);

        return new ReservationResponse(
                reservation.getId(),
                reservation.getTitle(),
                reservation.getStatus(),
                RoomSummaryResponse.from(reservation.getRoom()),
                OrganizerSummaryResponse.from(reservation.getOrganizer()),
                reservation.getStart().atOffset(ZoneOffset.UTC),
                reservation.getEnd().atOffset(ZoneOffset.UTC),
                reservation.getNumberOfParticipants(),
                equipmentCodes,
                reservation.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
