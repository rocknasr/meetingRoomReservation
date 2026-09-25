package reservation.model;

/**
 * Etat d'exploitation d'une salle.
 *
 * <p>Une salle en {@link #MAINTENANCE} n'est ni proposee par la recherche de
 * disponibilite ni reservable.</p>
 */
public enum RoomStatus {
    AVAILABLE,
    MAINTENANCE
}
