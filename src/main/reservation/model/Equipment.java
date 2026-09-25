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
 * Equipement pouvant etre installe dans une salle et exige par une reservation.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "EQUIPMENT")
@EqualsAndHashCode(of = "id")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Column(name = "LABEL", nullable = false)
    private String label;

    public Equipment(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
