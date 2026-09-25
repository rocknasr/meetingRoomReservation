package reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Salle de reunion, localisee dans un batiment et a un etage donne.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ROOM")
@EqualsAndHashCode(of = "id")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "BUILDING_ID", nullable = false)
    private Building building;

    @Column(name = "FLOOR", nullable = false)
    private int floor;

    /** Nombre maximal de participants pouvant etre accueillis. */
    @Column(name = "CAPACITY", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "ROOM_EQUIPMENT",
            joinColumns = @JoinColumn(name = "ROOM_ID"),
            inverseJoinColumns = @JoinColumn(name = "EQUIPMENT_ID"))
    private Set<Equipment> equipment = new LinkedHashSet<>();

    public Room(String name, Building building, int floor, int capacity) {
        this.name = name;
        this.building = building;
        this.floor = floor;
        this.capacity = capacity;
        this.status = RoomStatus.AVAILABLE;
    }

    /**
     * Indique si la salle peut etre proposee ou reservee.
     *
     * @return vrai lorsque la salle n'est pas en maintenance
     */
    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    /**
     * Retourne les codes des equipements installes dans la salle.
     *
     * @return l'ensemble des codes, eventuellement vide
     */
    public Set<String> getEquipmentCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (Equipment item : equipment) {
            codes.add(item.getCode());
        }
        return codes;
    }

    /**
     * Verifie que la salle possede tous les equipements demandes. La presence
     * d'equipements supplementaires est autorisee.
     *
     * @param requiredCodes codes exiges, eventuellement vides
     * @return vrai lorsque aucun equipement ne manque
     */
    public boolean hasAllEquipment(Set<String> requiredCodes) {
        return getEquipmentCodes().containsAll(requiredCodes);
    }

    /**
     * Liste les equipements exiges que la salle ne possede pas.
     *
     * @param requiredCodes codes exiges, eventuellement vides
     * @return les codes manquants, dans l'ordre de la demande
     */
    public Set<String> findMissingEquipment(Set<String> requiredCodes) {
        Set<String> installed = getEquipmentCodes();

        Set<String> missing = new LinkedHashSet<>();
        for (String code : requiredCodes) {
            if (!installed.contains(code)) {
                missing.add(code);
            }
        }
        return missing;
    }
}
