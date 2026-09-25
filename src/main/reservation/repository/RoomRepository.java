package reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import reservation.model.Room;

import java.util.List;

/**
 * Acces aux salles.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * @param name nom a tester
     * @return vrai si une salle porte deja ce nom, sans tenir compte de la casse
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * @param name nom a tester
     * @param id   identifiant de la salle a exclure de la recherche
     * @return vrai si une autre salle porte deja ce nom
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Retourne les salles d'un batiment, pour verifier qu'une reduction du nombre
     * d'etages ne supprime aucune salle existante.
     *
     * @param buildingId identifiant du batiment
     * @return les salles de ce batiment
     */
    List<Room> findByBuildingId(Long buildingId);
}
