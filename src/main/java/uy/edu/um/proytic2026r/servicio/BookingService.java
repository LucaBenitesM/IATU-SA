package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.entities.Booking;
import uy.edu.um.proytic2026r.repositorio.ActivityInstanceRepository;
import uy.edu.um.proytic2026r.repositorio.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ActivityInstanceRepository activityInstanceRepository;

    @Transactional
    public Booking createBooking(Booking booking) {
        ActivityInstance instance = activityInstanceRepository.findById(booking.getActivityInstance().getId())
                .orElseThrow(() -> new IllegalArgumentException("La instancia de la actividad no existe."));

        // 1. Validar tiempo límite previo a la reserva
        int minutesDeadline = instance.getMinutesBeforeBookingDeadline() != null ? instance.getMinutesBeforeBookingDeadline() : 0;
        LocalDateTime deadline = instance.getDateTime().minusMinutes(minutesDeadline);

        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalStateException("El tiempo límite para reservar esta actividad ha expirado.");
        }

        // 2. Validar cupos disponibles
        if (instance.getAvailablePlaces() < booking.getParticipantsCount()) {
            throw new IllegalStateException("No hay suficientes lugares disponibles para esta cantidad de participantes.");
        }

        // 3. Descontar los cupos y actualizar la instancia
        instance.setAvailablePlaces(instance.getAvailablePlaces() - booking.getParticipantsCount());
        activityInstanceRepository.save(instance);

        // 4. Guardar la reserva
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(Booking.BookingStatus.PENDING_PAYMENT);

        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsByTourist(Long touristId) {
        return bookingRepository.findByTouristId(touristId);
    }
}
