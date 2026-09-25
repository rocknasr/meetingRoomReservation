package reservation.exception;

/**
 * Periode inversee, situee dans le passe ou trop longue : repond en 400 avec le
 * code {@code INVALID_RESERVATION_PERIOD}.
 */
public class InvalidReservationPeriodException extends ApiException {

    public InvalidReservationPeriodException(String message) {
        super(ApiErrorCode.INVALID_RESERVATION_PERIOD, message);
    }

    /**
     * @return l'erreur signalant que le debut n'est pas anterieur a la fin
     */
    public static InvalidReservationPeriodException endBeforeStart() {
        return new InvalidReservationPeriodException(
                "La date de debut doit etre anterieure a la date de fin");
    }

    /**
     * @return l'erreur signalant que la periode commence dans le passe
     */
    public static InvalidReservationPeriodException startInThePast() {
        return new InvalidReservationPeriodException(
                "La date de debut ne peut pas etre dans le passe");
    }

    /**
     * @return l'erreur signalant que la duree depasse huit heures
     */
    public static InvalidReservationPeriodException tooLong() {
        return new InvalidReservationPeriodException(
                "Une reservation ne peut pas depasser huit heures");
    }

    /**
     * @return l'erreur signalant un intervalle de filtrage incoherent
     */
    public static InvalidReservationPeriodException invalidFilterRange() {
        return new InvalidReservationPeriodException(
                "Le parametre from doit etre anterieur au parametre to");
    }
}
