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
        // GIVEN une periode de deux heures situee le lendemain
        Instant start = tomorrow;
        Instant end = tomorrow.plusSeconds(2 * ONE_HOUR);

        // WHEN on la valide
        // THEN aucune erreur n'est levee
        assertDoesNotThrow(() -> periodValidator.validateForReservation(start, end));
    }

    @Test
    @DisplayName("Une periode dont le debut n'est pas anterieur a la fin est refusee")
    void shouldRejectPeriodWhereStartIsNotBeforeEnd() {
        // GIVEN une periode de duree nulle
        Instant start = tomorrow;

        // WHEN on la valide
        InvalidReservationPeriodException exception = assertThrows(
                InvalidReservationPeriodException.class,
                () -> periodValidator.validateForReservation(start, start));

        // THEN le refus porte sur l'ordre des dates
        assertEquals("La date de debut doit etre anterieure a la date de fin", exception.getMessage());
    }

    @Test
    @DisplayName("Une periode commencant dans le passe est refusee")
    void shouldRejectPeriodStartingInThePast() {
        // GIVEN une periode commencant une heure avant l'instant courant
        Instant start = Instant.now().minusSeconds(ONE_HOUR);
        Instant end = Instant.now().plusSeconds(ONE_HOUR);

        // WHEN on la valide
        InvalidReservationPeriodException exception = assertThrows(
                InvalidReservationPeriodException.class,
                () -> periodValidator.validateForReservation(start, end));

        // THEN le refus porte sur le debut dans le passe
        assertEquals("La date de debut ne peut pas etre dans le passe", exception.getMessage());
    }

    @Test
    @DisplayName("Une duree de huit heures exactement est acceptee")
    void shouldAcceptPeriodOfExactlyEightHours() {
        // GIVEN une periode future de huit heures
        Instant start = tomorrow;
        Instant end = tomorrow.plusSeconds(8 * ONE_HOUR);

        // WHEN on la valide
        // THEN la duree maximale n'est pas depassee
        assertDoesNotThrow(() -> periodValidator.validateForReservation(start, end));
    }

    @Test
    @DisplayName("Une duree superieure a huit heures est refusee")
    void shouldRejectPeriodLongerThanEightHours() {
        // GIVEN une periode future de huit heures et une minute
        Instant start = tomorrow;
        Instant end = tomorrow.plusSeconds(8 * ONE_HOUR + 60);

        // WHEN on la valide
        InvalidReservationPeriodException exception = assertThrows(
                InvalidReservationPeriodException.class,
                () -> periodValidator.validateForReservation(start, end));

        // THEN le refus porte sur la duree maximale
        assertEquals("Une reservation ne peut pas depasser huit heures", exception.getMessage());
    }

    @Test
    @DisplayName("Des bornes de filtrage absentes sont acceptees")
    void shouldAcceptMissingFilterBounds() {
        // GIVEN aucune borne de filtrage
        // WHEN on valide l'intervalle
        // THEN aucune erreur n'est levee
        assertDoesNotThrow(() -> periodValidator.validateFilterRange(null, null));
    }

    @Test
    @DisplayName("Un intervalle de filtrage inverse est refuse")
    void shouldRejectReversedFilterRange() {
        // GIVEN une borne basse posterieure a la borne haute
        Instant from = tomorrow.plusSeconds(24 * ONE_HOUR);
        Instant to = tomorrow;

        // WHEN on valide l'intervalle
        InvalidReservationPeriodException exception = assertThrows(
                InvalidReservationPeriodException.class,
                () -> periodValidator.validateFilterRange(from, to));

        // THEN le refus nomme les parametres fautifs
        assertEquals("Le parametre from doit etre anterieur au parametre to", exception.getMessage());
    }
}
