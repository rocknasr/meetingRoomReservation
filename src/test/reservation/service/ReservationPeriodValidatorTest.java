package reservation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reservation.exception.InvalidReservationPeriodException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitaires des regles de periode.
 *
 * <p>Les dates sont exprimees par rapport a l'instant courant, afin que les tests
 * restent valables quelle que soit la date d'execution.</p>
 */
class ReservationPeriodValidatorTest {

    private static final long ONE_HOUR = 3600L;

    private ReservationPeriodValidator periodValidator;

    private Instant tomorrow;

    @BeforeEach
    void setUp() {
        periodValidator = new ReservationPeriodValidator();
        tomorrow = Instant.now().plusSeconds(24 * ONE_HOUR);
    }

    @Test
    @DisplayName("Une periode future de deux heures est acceptee")
    void shouldAcceptValidPeriod() {
        Instant start = tomorrow;
        Instant end = tomorrow.plusSeconds(2 * ONE_HOUR);

        assertDoesNotThrow(() -> periodValidator.validateForReservation(start, end));
    }

    @Test
    @DisplayName("Une periode dont le debut n'est pas anterieur a la fin est refusee")
    void shouldRejectPeriodWhereStartIsNotBeforeEnd() {
        Instant start = tomorrow;

        InvalidReservationPeriodException exception = assertThrows(
                InvalidReservationPeriodException.class,
                () -> periodValidator.validateForReservation(start, start));

        assertEquals("La date de debut doit etre anterieure a la date de fin", exception.getMessage());
    }

    @Test
    @DisplayName("Une periode commencant dans le passe est refusee")
    void shouldRejectPeriodStartingInThePast() {
        Instant start = Instant.now().minusSeconds(ONE_HOUR);
        Instant end = Instant.now().plusSeconds(ONE_HOUR);

        InvalidReservationPeriodException exception = assertThrows(
                InvalidReservationPeriodException.class,
                () -> periodValidator.validateForReservation(start, end));

        assertEquals("La date de debut ne peut pas etre dans le passe", exception.getMessage());
    }

    @Test
    @DisplayName("Une duree de huit heures exactement est acceptee")
    void shouldAcceptPeriodOfExactlyEightHours() {
        Instant start = tomorrow;
        Instant end = tomorrow.plusSeconds(8 * ONE_HOUR);

        assertDoesNotThrow(() -> periodValidator.validateForReservation(start, end));
    }

    @Test
    @DisplayName("Une duree superieure a huit heures est refusee")
    void shouldRejectPeriodLongerThanEightHours() {
        Instant start = tomorrow;
        Instant end = tomorrow.plusSeconds(8 * ONE_HOUR + 60);

        InvalidReservationPeriodException exception = assertThrows(
                InvalidReservationPeriodException.class,
                () -> periodValidator.validateForReservation(start, end));

        assertEquals("Une reservation ne peut pas depasser huit heures", exception.getMessage());
    }

    @Test
    @DisplayName("Des bornes de filtrage absentes sont acceptees")
    void shouldAcceptMissingFilterBounds() {
        assertDoesNotThrow(() -> periodValidator.validateFilterRange(null, null));
    }

    @Test
    @DisplayName("Un intervalle de filtrage inverse est refuse")
    void shouldRejectReversedFilterRange() {
        Instant from = tomorrow.plusSeconds(24 * ONE_HOUR);
        Instant to = tomorrow;

        InvalidReservationPeriodException exception = assertThrows(
                InvalidReservationPeriodException.class,
                () -> periodValidator.validateFilterRange(from, to));

        assertEquals("Le parametre from doit etre anterieur au parametre to", exception.getMessage());
    }
}
