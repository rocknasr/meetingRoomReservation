package reservation.model;

/**
 * Etat d'une reservation.
 *
 * <p>Une reservation {@link #CANCELLED} reste consultable mais ne bloque plus la
 * salle : elle est ignoree lors de la detection des chevauchements.</p>
 */
public enum ReservationStatus {
    CONFIRMED,
    CANCELLED
}
