package reservation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import reservation.model.RoomStatus;

/** Corps de la requete de changement de statut d'une salle. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomStatusRequest {

    @NotNull
    private RoomStatus status;
}
