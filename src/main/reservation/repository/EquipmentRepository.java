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

    /**
     * @param code code technique recherche
     * @return l'equipement correspondant, s'il existe
     */
    Optional<Equipment> findByCode(String code);

    /**
     * @param code code a tester
     * @return vrai si un equipement porte deja ce code
     */
    boolean existsByCodeIgnoreCase(String code);
}
