package reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Batiment accueillant des salles et des organisateurs.
 *
 * <p>Le rez-de-chaussee porte le numero {@code 0}. Pour un batiment de
 * {@code numberOfFloors} etages, un etage valide est donc compris entre
 * {@code 0} et {@code numberOfFloors - 1}.</p>
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "BUILDING")
@EqualsAndHashCode(of = "id")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "NUMBER_OF_FLOORS", nullable = false)
    private int numberOfFloors;

    public Building(String name, int numberOfFloors) {
        this.name = name;
        this.numberOfFloors = numberOfFloors;
    }

    public boolean hasFloor(int floor) {
        return floor >= 0 && floor < numberOfFloors;
    }
}
