package reservation.controller;

import reservation.dto.ReservationRequest;
import reservation.dto.ReservationResponse;
import reservation.model.Reservation;
import reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoints de gestion des reservations.
 */
@Validated
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        return created(reservationService.create(request));
    }

    @PostMapping("/automatic")
    public ResponseEntity<ReservationResponse> createAutomaticReservation(
            @Valid @RequestBody ReservationRequest request) {
        return created(reservationService.createAutomatic(request));
    }

    @GetMapping
    public List<ReservationResponse> listReservations(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        List<Reservation> reservations = reservationService.search(
                roomId, organizerId, toInstant(from), toInstant(to));

        List<ReservationResponse> response = new ArrayList<>();
        for (Reservation reservation : reservations) {
            response.add(ReservationResponse.from(reservation));
        }
        return response;
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse getReservation(@PathVariable Long reservationId) {
        return ReservationResponse.from(reservationService.findById(reservationId));
    }

    @PatchMapping("/{reservationId}/cancel")
    public ReservationResponse cancelReservation(@PathVariable Long reservationId) {
        return ReservationResponse.from(reservationService.cancel(reservationId));
    }

    private ResponseEntity<ReservationResponse> created(Reservation reservation) {
        ReservationResponse body = ReservationResponse.from(reservation);
        return ResponseEntity.created(URI.create("/api/reservations/" + body.getId())).body(body);
    }

    private Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
