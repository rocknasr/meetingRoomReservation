package reservation.repository;

import reservation.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acces aux batiments.
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    /**
     * @param name nom a tester
     * @return vrai si un batiment porte deja ce nom, sans tenir compte de la casse
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * @param name nom a tester
     * @param id   identifiant du batiment a exclure de la recherche
     * @return vrai si un autre batiment porte deja ce nom
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
