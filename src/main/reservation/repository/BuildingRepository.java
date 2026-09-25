package reservation.repository;

import reservation.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acces aux batiments.
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
