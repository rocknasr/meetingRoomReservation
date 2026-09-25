package reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import reservation.model.Organizer;

import java.util.List;

/**
 * Acces aux organisateurs.
 */
@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    List<Organizer> findByBuildingId(Long buildingId);
}
