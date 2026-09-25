package reservation.repository;

import reservation.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acces aux equipements.
 */
@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);
}
