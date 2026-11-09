package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Booking;
import uy.edu.um.proytic2026r.entities.Payment;
import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.ActivityInstanceRepository;
import uy.edu.um.proytic2026r.repositorio.BookingRepository;
import uy.edu.um.proytic2026r.repositorio.PaymentRepository;
import uy.edu.um.proytic2026r.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ActivityInstanceRepository activityInstanceRepository;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PaymentRepository pagoRepositorio;
    private final PaymentGatewayService pasarelaPago;

    public BookingService(BookingRepository bookingRepository,
                          ActivityInstanceRepository activityInstanceRepository,
                          UsuarioRepositorio usuarioRepositorio,
                          PaymentRepository pagoRepositorio,
                          PaymentGatewayService pasarelaPago) {
        this.bookingRepository = bookingRepository;
        this.activityInstanceRepository = activityInstanceRepository;
        this.usuarioRepositorio = usuarioRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.pasarelaPago = pasarelaPago;
    }

    @Transactional
    public Booking createBooking(Booking booking) {
        if (booking.getTourist() == null || booking.getTourist().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta indicar el turista.");
        }
        if (booking.getActivityInstance() == null || booking.getActivityInstance().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta indicar la fecha y horario de la actividad.");
        }
        Integer cantidad = booking.getParticipantsCount();
        if (cantidad == null || cantidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de participantes debe ser al menos 1.");
        }

        Usuario tourist = usuarioRepositorio.findById(booking.getTourist().getId()).orElse(null);
        if (tourist == null || !"TOURIST".equals(tourist.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los turistas registrados pueden reservar.");
        }

        ActivityInstance instance = activityInstanceRepository
                .findByIdForUpdate(booking.getActivityInstance().getId()).orElse(null);
        if (instance == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La fecha y horario de la actividad no existe.");
        }

        int minutosLimite = instance.getMinutesBeforeBookingDeadline() != null ? instance.getMinutesBeforeBookingDeadline() : 0;
        LocalDateTime limite = instance.getDateTime().minusMinutes(minutosLimite);
        if (LocalDateTime.now().isAfter(limite)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El tiempo límite para reservar esta actividad ya pasó.");
        }

        if (instance.getAvailablePlaces() < cantidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay lugares suficientes. Quedan " + instance.getAvailablePlaces() + ".");
        }

        instance.setAvailablePlaces(instance.getAvailablePlaces() - cantidad);
        activityInstanceRepository.save(instance);

        Booking nueva = new Booking();
        nueva.setTourist(tourist);
        nueva.setActivityInstance(instance);
        nueva.setParticipantsCount(cantidad);
        nueva.setTotalPrice(instance.getAtraccion().getPrice().multiply(BigDecimal.valueOf(cantidad)));
        nueva.setBookingDate(LocalDateTime.now());
        nueva.setStatus(Booking.BookingStatus.PENDING_PAYMENT);
        return bookingRepository.save(nueva);
    }

    public Booking pay(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La reserva no existe.");
        }
        if (booking.getStatus() != Booking.BookingStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta reserva no está pendiente de pago.");
        }
        if (booking.getActivityInstance().getDateTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La actividad ya comenzó, no se puede pagar.");
        }

        String resultado;
        try {
            resultado = pasarelaPago.cobrar(booking.getTotalPrice());
        } catch (PaymentGatewayService.NoDisponibleException e) {
            guardarPago(booking, Payment.ERROR, null, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de pagos no está disponible. Tu reserva sigue pendiente, probá de nuevo en unos minutos.");
        }

        if ("APPROVED".equals(resultado)) {
            String referencia = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            guardarPago(booking, Payment.APPROVED, referencia, "Pago aprobado");
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            return bookingRepository.save(booking);
        }

        guardarPago(booking, Payment.REJECTED, null, "Pago rechazado");
        throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                "Pago rechazado. Tu reserva sigue pendiente, podés intentar de nuevo.");
    }

    private void guardarPago(Booking booking, String estado, String referencia, String mensaje) {
        Payment pago = new Payment();
        pago.setBooking(booking);
        pago.setAmount(booking.getTotalPrice());
        pago.setStatus(estado);
        pago.setExternalReference(referencia);
        pago.setMessage(mensaje);
        pago.setPaymentDate(LocalDateTime.now());
        pagoRepositorio.save(pago);
    }

    @Transactional
    public Booking cancel(Long bookingId, Long touristId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La reserva no existe.");
        }
        if (booking.getTourist() == null || !booking.getTourist().getId().equals(touristId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta reserva no es tuya.");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED || booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta reserva ya no se puede cancelar.");
        }

        ActivityInstance instance = activityInstanceRepository
                .findByIdForUpdate(booking.getActivityInstance().getId()).orElse(null);

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            Atraccion atraccion = instance.getAtraccion();
            if (atraccion.getAllowsCancellation() == null || !atraccion.getAllowsCancellation()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta actividad no permite cancelación.");
            }
            int minutos = atraccion.getCancellationDeadlineMinutes() != null ? atraccion.getCancellationDeadlineMinutes() : 0;
            LocalDateTime limite = instance.getDateTime().minusMinutes(minutos);
            if (LocalDateTime.now().isAfter(limite)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya pasó el plazo para cancelar esta reserva.");
            }
            int porcentaje = atraccion.getRefundPercentage() != null ? atraccion.getRefundPercentage() : 0;
            BigDecimal reembolso = booking.getTotalPrice()
                    .multiply(BigDecimal.valueOf(porcentaje))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            booking.setRefundAmount(reembolso);
        }

        instance.setAvailablePlaces(instance.getAvailablePlaces() + booking.getParticipantsCount());
        activityInstanceRepository.save(instance);

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsByTourist(Long touristId) {
        return bookingRepository.findByTouristId(touristId);
    }

    public List<Booking> getBookingsByProviderOwner(Long ownerId) {
        return bookingRepository.findByActivityInstance_Atraccion_Prestador_Owner_Id(ownerId);
    }
}
