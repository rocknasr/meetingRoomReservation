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

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByRoomIdAndStatus(Long roomId, ReservationStatus status);
}
