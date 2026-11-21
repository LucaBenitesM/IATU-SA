package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Booking;
import uy.edu.um.proytic2026r.entities.Participant;
import uy.edu.um.proytic2026r.entities.Payment;
import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.ActivityInstanceRepository;
import uy.edu.um.proytic2026r.repositorio.BookingRepository;
import uy.edu.um.proytic2026r.repositorio.PaymentRepository;
import uy.edu.um.proytic2026r.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
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

        List<Participant> participantes = validarParticipantes(booking.getParticipants(), cantidad,
                instance.getAtraccion(), instance.getDateTime());

        instance.setAvailablePlaces(instance.getAvailablePlaces() - cantidad);
        activityInstanceRepository.save(instance);

        Booking nueva = new Booking();
        nueva.setTourist(tourist);
        nueva.setActivityInstance(instance);
        nueva.setParticipantsCount(cantidad);
        nueva.setTotalPrice(instance.getAtraccion().getPrice().multiply(BigDecimal.valueOf(cantidad)));
        nueva.setBookingDate(LocalDateTime.now());
        nueva.setStatus(Booking.BookingStatus.PENDING_PAYMENT);
        // 24 horas para pagar, pero nunca despues del inicio de la actividad
        LocalDateTime plazo = LocalDateTime.now().plusHours(24);
        if (instance.getDateTime().isBefore(plazo)) {
            plazo = instance.getDateTime();
        }
        nueva.setPaymentDeadline(plazo);
        for (Participant p : participantes) {
            p.setBooking(nueva);
        }
        nueva.setParticipants(participantes);
        return bookingRepository.save(nueva);
    }

    private List<Participant> validarParticipantes(List<Participant> recibidos, int cantidad, Atraccion atraccion, LocalDateTime inicio) {
        if (recibidos == null || recibidos.size() != cantidad) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los datos de los participantes no coinciden con la cantidad de personas.");
        }
        List<Participant> limpios = new ArrayList<Participant>();
        Set<String> cedulas = new HashSet<String>();
        for (Participant p : recibidos) {
            if (vacio(p.getFirstName()) || vacio(p.getLastName()) || vacio(p.getDocumentId()) || p.getBirthDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completá los datos de todos los participantes.");
            }
            String cedula = p.getDocumentId().replaceAll("\\D", "");
            if (cedula.length() < 7 || cedula.length() > 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cédula debe tener 7 u 8 dígitos.");
            }
            if (!cedulas.add(cedula)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay cédulas repetidas entre los participantes.");
            }
            if (p.getBirthDate().isAfter(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento no es válida.");
            }
            Integer minima = atraccion.getMinimumAge();
            if (minima != null && Period.between(p.getBirthDate(), inicio.toLocalDate()).getYears() < minima) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        p.getFirstName().trim() + " " + p.getLastName().trim() + " no cumple la edad mínima de " + minima + " años.");
            }
            Participant limpio = new Participant();
            limpio.setFirstName(p.getFirstName().trim());
            limpio.setLastName(p.getLastName().trim());
            limpio.setDocumentId(cedula);
            limpio.setBirthDate(p.getBirthDate());
            limpios.add(limpio);
        }
        return limpios;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    // Cada minuto: las reservas que siguen pendientes pasado su plazo de pago se cancelan y liberan los lugares
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelarReservasVencidas() {
        List<Booking> vencidas = bookingRepository.findByStatusAndPaymentDeadlineBefore(
                Booking.BookingStatus.PENDING_PAYMENT, LocalDateTime.now());
        for (Booking b : vencidas) {
            ActivityInstance instance = activityInstanceRepository
                    .findByIdForUpdate(b.getActivityInstance().getId()).orElse(null);
            if (instance != null) {
                instance.setAvailablePlaces(instance.getAvailablePlaces() + b.getParticipantsCount());
                activityInstanceRepository.save(instance);
            }
            b.setStatus(Booking.BookingStatus.CANCELLED);
            b.setCancelReason("El plazo de pago de 24 horas venció.");
            bookingRepository.save(b);
        }
    }

    public Booking pay(Long bookingId, Long touristId, Map<String, Object> datosPago) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La reserva no existe.");
        }
        if (booking.getTourist() == null || touristId == null || !booking.getTourist().getId().equals(touristId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta reserva no es tuya.");
        }
        if (booking.getStatus() != Booking.BookingStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta reserva no está pendiente de pago.");
        }
        if (booking.getActivityInstance().getDateTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La actividad ya comenzó, no se puede pagar.");
        }
        if (booking.getPaymentDeadline() != null && booking.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El plazo para pagar esta reserva venció.");
        }
        if (datosPago == null || datosPago.get("token") == null || datosPago.get("payment_method_id") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan los datos de la tarjeta.");
        }

        // El monto sale siempre de la reserva, nunca de lo que mande el navegador
        String descripcion = "Reserva en Uruguay Mnatural: " + booking.getActivityInstance().getAtraccion().getName();
        PaymentGatewayService.Resultado resultado;
        try {
            resultado = pasarelaPago.cobrar(booking.getTotalPrice(), descripcion, "reserva-" + booking.getId(), datosPago);
        } catch (PaymentGatewayService.NoDisponibleException e) {
            guardarPago(booking, Payment.ERROR, null, null, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de pagos no está disponible. Tu reserva sigue pendiente, probá de nuevo en unos minutos.");
        }

        if (Payment.APPROVED.equals(resultado.getEstado())) {
            Booking actual = bookingRepository.findById(bookingId).orElse(booking);
            if (actual.getStatus() != Booking.BookingStatus.PENDING_PAYMENT) {
                // La reserva venció mientras se procesaba el pago: se devuelve el dinero
                try {
                    pasarelaPago.reembolsar(resultado.getIdExterno(), booking.getTotalPrice());
                } catch (PaymentGatewayService.NoDisponibleException e) {
                    guardarPago(booking, Payment.ERROR, resultado.getIdExterno(), resultado.getUltimos4(),
                            "Pago aprobado sobre una reserva vencida: falta reembolsar. " + e.getMessage());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El plazo para pagar esta reserva venció.");
                }
                guardarPago(booking, Payment.REJECTED, resultado.getIdExterno(), resultado.getUltimos4(),
                        "Pago reembolsado: la reserva venció durante el pago");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El plazo para pagar esta reserva venció.");
            }
            guardarPago(booking, Payment.APPROVED, resultado.getIdExterno(), resultado.getUltimos4(), "Pago aprobado");
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            return bookingRepository.save(booking);
        }

        if (Payment.IN_PROCESS.equals(resultado.getEstado())) {
            guardarPago(booking, Payment.IN_PROCESS, resultado.getIdExterno(), resultado.getUltimos4(), "Pago en revisión");
            return booking;
        }

        guardarPago(booking, Payment.REJECTED, resultado.getIdExterno(), resultado.getUltimos4(),
                "Pago rechazado: " + resultado.getDetalle());
        throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, mensajeRechazo(resultado.getDetalle()));
    }

    private String mensajeRechazo(String detalle) {
        String d = detalle == null ? "" : detalle;
        if (d.contains("insufficient_amount")) {
            return "Pago rechazado: fondos insuficientes. Tu reserva sigue pendiente, podés intentar de nuevo.";
        }
        if (d.contains("security_code")) {
            return "Pago rechazado: código de seguridad incorrecto. Tu reserva sigue pendiente, podés intentar de nuevo.";
        }
        if (d.contains("bad_filled_date")) {
            return "Pago rechazado: fecha de vencimiento incorrecta. Tu reserva sigue pendiente, podés intentar de nuevo.";
        }
        if (d.contains("bad_filled")) {
            return "Pago rechazado: datos de la tarjeta incorrectos. Tu reserva sigue pendiente, podés intentar de nuevo.";
        }
        if (d.contains("card_disabled") || d.contains("call_for_authorize")) {
            return "Pago rechazado: tu banco no autorizó la operación. Tu reserva sigue pendiente, podés intentar de nuevo.";
        }
        return "Pago rechazado. Tu reserva sigue pendiente, podés intentar de nuevo.";
    }

    public List<Payment> getPayments(Long bookingId) {
        return pagoRepositorio.findByBookingId(bookingId);
    }

    private void guardarPago(Booking booking, String estado, String referencia, String ultimos4, String mensaje) {
        Payment pago = new Payment();
        pago.setBooking(booking);
        pago.setAmount(booking.getTotalPrice());
        pago.setStatus(estado);
        pago.setExternalReference(referencia);
        pago.setCardLast4(ultimos4);
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
            if (reembolso.compareTo(BigDecimal.ZERO) > 0) {
                reembolsarEnPasarela(booking, reembolso);
            }
            booking.setRefundAmount(reembolso);
        }

        instance.setAvailablePlaces(instance.getAvailablePlaces() + booking.getParticipantsCount());
        activityInstanceRepository.save(instance);

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    // Devuelve el dinero al medio de pago original usando el pago aprobado de la reserva
    private void reembolsarEnPasarela(Booking booking, BigDecimal monto) {
        for (Payment pago : pagoRepositorio.findByBookingId(booking.getId())) {
            if (Payment.APPROVED.equals(pago.getStatus()) && pago.getExternalReference() != null) {
                try {
                    pasarelaPago.reembolsar(pago.getExternalReference(), monto);
                } catch (PaymentGatewayService.NoDisponibleException e) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "No pudimos procesar el reembolso. Tu reserva no se canceló, probá de nuevo en unos minutos.");
                }
                return;
            }
        }
    }

    public List<Booking> getBookingsByTourist(Long touristId) {
        return bookingRepository.findByTouristId(touristId);
    }

    public List<Booking> getBookingsByProviderOwner(Long ownerId) {
        return bookingRepository.findByActivityInstance_Atraccion_Prestador_Owner_Id(ownerId);
    }
}
