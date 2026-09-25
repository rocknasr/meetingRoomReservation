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

    /**
     * @param email adresse a tester
     * @return vrai si un organisateur utilise deja cette adresse
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Retourne les organisateurs rattaches a un batiment.
     *
     * @param buildingId identifiant du batiment
     * @return les organisateurs de ce batiment
     */
    List<Organizer> findByBuildingId(Long buildingId);
}
