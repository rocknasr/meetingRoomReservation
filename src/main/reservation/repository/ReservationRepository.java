package reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import reservation.model.Reservation;
import reservation.model.ReservationStatus;

import java.util.List;

/**
 * Acces aux reservations.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Retourne toutes les reservations d'un statut donne, toutes salles confondues.
     *
     * @param status statut recherche
     * @return les reservations correspondantes
     */
    List<Reservation> findByStatus(ReservationStatus status);

    /**
     * Retourne les reservations d'un statut donne pour une salle.
     *
     * @param roomId identifiant de la salle
     * @param status statut recherche
     * @return les reservations correspondantes
     */
    List<Reservation> findByRoomIdAndStatus(Long roomId, ReservationStatus status);
}
