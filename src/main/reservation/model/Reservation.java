package reservation.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Occupation d'une salle par un organisateur sur une periode donnee.
 *
 * <p>Les instants sont conserves en UTC : le decalage horaire recu en entree est
 * normalise a l'enregistrement, et les reponses exposent les dates au format
 * ISO 8601.</p>
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RESERVATION")
@EqualsAndHashCode(of = "id")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ROOM_ID", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ORGANIZER_ID", nullable = false)
    private Organizer organizer;

    @Column(name = "START_AT", nullable = false)
    private Instant start;

    @Column(name = "END_AT", nullable = false)
    private Instant end;

    @Column(name = "NUMBER_OF_PARTICIPANTS", nullable = false)
    private int numberOfParticipants;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "RESERVATION_EQUIPMENT",
            joinColumns = @JoinColumn(name = "RESERVATION_ID"))
    @Column(name = "EQUIPMENT_CODE", nullable = false)
    private Set<String> requiredEquipmentCodes = new LinkedHashSet<>();

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;

    public boolean isConfirmed() {
        return status == ReservationStatus.CONFIRMED;
    }

    // Bornes exclusives : une reunion peut commencer a l'instant ou la precedente se termine.
    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }
}
