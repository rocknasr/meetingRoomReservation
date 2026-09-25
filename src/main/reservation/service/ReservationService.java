package reservation.service;

import reservation.dto.ReservationRequest;
import reservation.exception.ConflictException;
import reservation.exception.ResourceNotFoundException;
import reservation.exception.ValidationException;
import reservation.model.Organizer;
import reservation.model.Reservation;
import reservation.model.ReservationStatus;
import reservation.model.Room;
import reservation.repository.ReservationRepository;
import reservation.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creation, consultation et annulation des reservations.
 *
 * <p>Les regles de compatibilite et de classement des salles sont deleguees au
 * {@link RoomAssignmentService} ; ce service se charge de l'ordre des controles,
 * des erreurs renvoyees et de la persistance.</p>
 */
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final OrganizerService organizerService;
    private final EquipmentService equipmentService;
    private final RoomAssignmentService roomAssignmentService;
    private final ReservationPeriodValidator periodValidator;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            RoomService roomService,
            OrganizerService organizerService,
            EquipmentService equipmentService,
            RoomAssignmentService roomAssignmentService,
            ReservationPeriodValidator periodValidator) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
        this.organizerService = organizerService;
        this.equipmentService = equipmentService;
        this.roomAssignmentService = roomAssignmentService;
        this.periodValidator = periodValidator;
    }

    public Reservation create(ReservationRequest request) {
        Instant start = request.getStart().toInstant();
        Instant end = request.getEnd().toInstant();
        periodValidator.validateForReservation(start, end);

        // Sans roomId, le client doit passer par POST /api/reservations/automatic.
        if (request.getRoomId() == null) {
            throw new ValidationException(Map.of("roomId", "est obligatoire"));
        }

        Organizer organizer = organizerService.findById(request.getOrganizerId());
        Room room = roomService.findById(request.getRoomId());
        Set<String> requiredEquipment = requiredEquipmentOf(request);
        equipmentService.checkAllExist(requiredEquipment);

        checkRoomAccepts(room, request.getNumberOfParticipants(), requiredEquipment, start, end);

        return save(request, room, organizer, start, end, requiredEquipment);
    }

    public Reservation createAutomatic(ReservationRequest request) {
        Instant start = request.getStart().toInstant();
        Instant end = request.getEnd().toInstant();
        periodValidator.validateForReservation(start, end);

        Organizer organizer = organizerService.findById(request.getOrganizerId());
        Set<String> requiredEquipment = requiredEquipmentOf(request);
        equipmentService.checkAllExist(requiredEquipment);

        // Seules les reservations confirmees bloquent une salle ; les annulees sont ignorees.
        List<Reservation> confirmed = reservationRepository.findByStatus(ReservationStatus.CONFIRMED);
        Room room = roomAssignmentService.selectBestRoom(
                        roomRepository.findAll(),
                        organizer,
                        request.getNumberOfParticipants(),
                        requiredEquipment,
                        confirmed,
                        start,
                        end)
                .orElseThrow(ConflictException::noCompatibleRoom);

        return save(request, room, organizer, start, end, requiredEquipment);
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.reservation(id));
    }

    public List<Reservation> search(Long roomId, Long organizerId, Instant from, Instant to) {
        periodValidator.validateFilterRange(from, to);

        List<Reservation> found = new ArrayList<>();
        for (Reservation reservation : reservationRepository.findAll()) {
            if (matchesFilters(reservation, roomId, organizerId, from, to)) {
                found.add(reservation);
            }
        }

        found.sort(Comparator
                .comparing(Reservation::getStart)
                .thenComparing(Reservation::getId));
        return found;
    }

    private boolean matchesFilters(
            Reservation reservation, Long roomId, Long organizerId, Instant from, Instant to) {
        if (roomId != null && !roomId.equals(reservation.getRoom().getId())) {
            return false;
        }
        if (organizerId != null && !organizerId.equals(reservation.getOrganizer().getId())) {
            return false;
        }
        // Une reservation est retenue des qu'elle chevauche l'intervalle [from, to[.
        if (from != null && !reservation.getEnd().isAfter(from)) {
            return false;
        }
        return to == null || reservation.getStart().isBefore(to);
    }

    public Reservation cancel(Long id) {
        Reservation reservation = findById(id);
        if (!reservation.isConfirmed()) {
            throw ConflictException.reservationAlreadyCancelled(id);
        }
        // Annulation logique : la reservation reste consultable avec le statut CANCELLED.
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    private void checkRoomAccepts(
            Room room, int numberOfParticipants, Set<String> requiredEquipment, Instant start, Instant end) {
        // L'ordre des controles decide quelle erreur est renvoyee si plusieurs regles echouent.
        if (!room.isAvailable()) {
            throw ConflictException.roomUnavailable(room);
        }
        if (room.getCapacity() < numberOfParticipants) {
            throw ConflictException.roomCapacityExceeded(room, numberOfParticipants);
        }
        Set<String> missing = room.findMissingEquipment(requiredEquipment);
        if (!missing.isEmpty()) {
            throw ConflictException.missingEquipment(room, missing);
        }
        List<Reservation> confirmed =
                reservationRepository.findByRoomIdAndStatus(room.getId(), ReservationStatus.CONFIRMED);
        for (Reservation existing : confirmed) {
            if (existing.overlaps(start, end)) {
                throw ConflictException.roomAlreadyReserved(room, existing);
            }
        }
    }

    private Set<String> requiredEquipmentOf(ReservationRequest request) {
        return request.getRequiredEquipmentCodes() == null ? Set.of() : request.getRequiredEquipmentCodes();
    }

    private Reservation save(
            ReservationRequest request,
            Room room,
            Organizer organizer,
            Instant start,
            Instant end,
            Set<String> requiredEquipment) {
        Reservation reservation = new Reservation();
        reservation.setTitle(request.getTitle());
        reservation.setRoom(room);
        reservation.setOrganizer(organizer);
        reservation.setStart(start);
        reservation.setEnd(end);
        reservation.setNumberOfParticipants(request.getNumberOfParticipants());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setRequiredEquipmentCodes(new LinkedHashSet<>(requiredEquipment));
        reservation.setCreatedAt(Instant.now());
        return reservationRepository.save(reservation);
    }
}
