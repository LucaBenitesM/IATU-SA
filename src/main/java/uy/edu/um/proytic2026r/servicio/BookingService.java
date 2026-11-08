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

    @Autowired
    private PaymentGatewayService paymentGatewayService; // Inyectamos el servicio de pago simulado

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

        // 3. Descontar temporalmente los cupos y actualizar la instancia
        instance.setAvailablePlaces(instance.getAvailablePlaces() - booking.getParticipantsCount());
        activityInstanceRepository.save(instance);

        // 4. Guardar la reserva inicialmente como PENDING_PAYMENT
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(Booking.BookingStatus.PENDING_PAYMENT);
        Booking savedBooking = bookingRepository.save(booking);

        // 5. Intentar procesar el pago con la pasarela externa
        try {
            boolean paymentApproved = paymentGatewayService.processPayment(savedBooking.getId(), savedBooking.getTotalPrice());

            if (paymentApproved) {
                // Si el pago es exitoso, confirmamos la reserva
                savedBooking.setStatus(Booking.BookingStatus.CONFIRMED);
            } else {
                // Si el pago es rechazado, cancelamos la reserva y devolvemos los cupos (Rollback manual de inventario)
                savedBooking.setStatus(Booking.BookingStatus.CANCELLED);
                instance.setAvailablePlaces(instance.getAvailablePlaces() + booking.getParticipantsCount());
                activityInstanceRepository.save(instance);
                throw new IllegalStateException("El pago fue rechazado por la entidad financiera. Reserva cancelada.");
            }

        } catch (RuntimeException e) {
            // Si la API externa no responde (Timeout / Caída del servicio - el 10% de simulación)
            // Se cancela la reserva y se devuelven los cupos para mantener la consistencia
            savedBooking.setStatus(Booking.BookingStatus.CANCELLED);
            instance.setAvailablePlaces(instance.getAvailablePlaces() + booking.getParticipantsCount());
            activityInstanceRepository.save(instance);

            // Re-lanzamos o informamos el error de que el servicio externo falló
            throw new RuntimeException("Error en pasarela de pagos: " + e.getMessage() + " La reserva ha sido cancelada y los cupos devueltos.");
        }

        return bookingRepository.save(savedBooking);
    }

    public List<Booking> getBookingsByTourist(Long touristId) {
        return bookingRepository.findByTouristId(touristId);
    }
}
