package reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Personne a l'origine d'une reservation.
 *
 * <p>Sa localisation (batiment et etage) sert au calcul de proximite lors de
 * l'attribution automatique d'une salle.</p>
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ORGANIZER")
@EqualsAndHashCode(of = "id")
public class Organizer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "EMAIL", nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "BUILDING_ID", nullable = false)
    private Building building;

    @Column(name = "FLOOR", nullable = false)
    private int floor;

    public Organizer(String name, String email, Building building, int floor) {
        this.name = name;
        this.email = email;
        this.building = building;
        this.floor = floor;
    }
}
