package reservation.service;

import reservation.exception.InvalidReservationPeriodException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Controle des regles de periode communes a toutes les demandes de reservation.
 */
@Service
public class ReservationPeriodValidator {

    public static final Duration MAX_DURATION = Duration.ofHours(8);

    public void validateChronology(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw InvalidReservationPeriodException.endBeforeStart();
        }
    }

    public void validateForReservation(Instant start, Instant end) {
        validateChronology(start, end);
        if (start.isBefore(Instant.now())) {
            throw InvalidReservationPeriodException.startInThePast();
        }
        if (Duration.between(start, end).compareTo(MAX_DURATION) > 0) {
            throw InvalidReservationPeriodException.tooLong();
        }
    }

    public void validateFilterRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw InvalidReservationPeriodException.invalidFilterRange();
        }
    }
}
