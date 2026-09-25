package reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree du backend de reservation de salles.
 */
@SpringBootApplication
public class MeetingRoomReservation {

    public static void main(String[] args) {
        SpringApplication.run(MeetingRoomReservation.class, args);
    }

}
