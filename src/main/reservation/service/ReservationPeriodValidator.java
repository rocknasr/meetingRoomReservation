package reservation.service;

import reservation.exception.InvalidReservationPeriodException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Controle des regles de periode communes a toutes les demandes de reservation.
 *
 */
@Service
public class ReservationPeriodValidator {

    /** Duree maximale d'une reservation. */
    public static final Duration MAX_DURATION = Duration.ofHours(8);

    /**
     * Verifie qu'une periode est exploitable : le debut doit etre strictement
     * anterieur a la fin.
     *
     * @param start debut de la periode
     * @param end   fin de la periode
     * @throws InvalidReservationPeriodException si la periode est inversee ou nulle
     */
    public void validateChronology(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw InvalidReservationPeriodException.endBeforeStart();
        }
    }

    /**
     * Verifie toutes les regles applicables a une reservation : ordre des dates,
     * debut dans le futur et duree maximale de huit heures.
     *
     * @param start debut de la periode
     * @param end   fin de la periode
     * @throws InvalidReservationPeriodException si une regle n'est pas respectee
     */
    public void validateForReservation(Instant start, Instant end) {
        validateChronology(start, end);
        if (start.isBefore(Instant.now())) {
            throw InvalidReservationPeriodException.startInThePast();
        }
        if (Duration.between(start, end).compareTo(MAX_DURATION) > 0) {
            throw InvalidReservationPeriodException.tooLong();
        }
    }

    /**
     * Verifie la coherence des bornes de filtrage des reservations.
     *
     * @param from borne basse, eventuellement absente
     * @param to   borne haute, eventuellement absente
     * @throws InvalidReservationPeriodException si {@code from} n'est pas anterieur a {@code to}
     */
    public void validateFilterRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw InvalidReservationPeriodException.invalidFilterRange();
        }
    }
}
