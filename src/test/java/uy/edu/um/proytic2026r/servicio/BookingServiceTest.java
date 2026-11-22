package uy.edu.um.proytic2026r.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Reglas de reservas: lugares, participantes, plazo de pago de 24 h, pago y cancelación con reembolso.
class BookingServiceTest {

    private BookingRepository bookings;
    private ActivityInstanceRepository instances;
    private UsuarioRepositorio usuarios;
    private PaymentRepository pagos;
    private PaymentGatewayService pasarela;
    private BookingService service;

    private Usuario turista;
    private Atraccion atraccion;
    private ActivityInstance instancia;

    @BeforeEach
    void preparar() {
        bookings = mock(BookingRepository.class);
        instances = mock(ActivityInstanceRepository.class);
        usuarios = mock(UsuarioRepositorio.class);
        pagos = mock(PaymentRepository.class);
        pasarela = mock(PaymentGatewayService.class);
        service = new BookingService(bookings, instances, usuarios, pagos, pasarela);

        turista = new Usuario();
        turista.setId(7L);
        turista.setRole("TOURIST");

        atraccion = new Atraccion();
        atraccion.setId(1L);
        atraccion.setName("Kayak");
        atraccion.setPrice(new BigDecimal("1000"));
        atraccion.setDurationMinutes(120);
        atraccion.setAllowsCancellation(true);
        atraccion.setCancellationDeadlineMinutes(60);
        atraccion.setRefundPercentage(50);

        instancia = new ActivityInstance();
        instancia.setId(5L);
        instancia.setAtraccion(atraccion);
        instancia.setDateTime(LocalDateTime.now().plusDays(3));
        instancia.setMaxCapacity(10);
        instancia.setAvailablePlaces(10);
        instancia.setMinutesBeforeBookingDeadline(0);

        when(usuarios.findById(7L)).thenReturn(Optional.of(turista));
        when(instances.findByIdForUpdate(5L)).thenReturn(Optional.of(instancia));
        when(bookings.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
    }

    private int codigo(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    private Participant persona(String nombre, String cedula, LocalDate nacimiento) {
        Participant p = new Participant();
        p.setFirstName(nombre);
        p.setLastName("Pérez");
        p.setDocumentId(cedula);
        p.setBirthDate(nacimiento);
        return p;
    }

    private Booking pedido(int cantidad, List<Participant> participantes) {
        Usuario t = new Usuario();
        t.setId(7L);
        ActivityInstance i = new ActivityInstance();
        i.setId(5L);
        Booking b = new Booking();
        b.setTourist(t);
        b.setActivityInstance(i);
        b.setParticipantsCount(cantidad);
        b.setParticipants(participantes);
        return b;
    }

    private List<Participant> adultos(int cantidad) {
        List<Participant> lista = new ArrayList<Participant>();
        for (int i = 0; i < cantidad; i++) {
            lista.add(persona("Persona" + i, "1234567" + i, LocalDate.of(1990, 1, 1)));
        }
        return lista;
    }

    private Booking reservaGuardada(Booking.BookingStatus estado) {
        Booking b = new Booking();
        b.setId(20L);
        b.setTourist(turista);
        b.setActivityInstance(instancia);
        b.setParticipantsCount(2);
        b.setTotalPrice(new BigDecimal("2000"));
        b.setStatus(estado);
        b.setPaymentDeadline(LocalDateTime.now().plusHours(10));
        when(bookings.findById(20L)).thenReturn(Optional.of(b));
        return b;
    }

    private Map<String, Object> tarjeta() {
        Map<String, Object> datos = new HashMap<String, Object>();
        datos.put("token", "SIM-4242424242424242");
        datos.put("payment_method_id", "visa");
        return datos;
    }

    // ---------- crear reserva ----------

    @Test
    void reservaDescuentaLugaresYQuedaPendienteDePago() {
        Booking b = service.createBooking(pedido(2, adultos(2)));

        assertEquals(Booking.BookingStatus.PENDING_PAYMENT, b.getStatus());
        assertEquals(8, instancia.getAvailablePlaces());
        assertEquals(0, new BigDecimal("2000").compareTo(b.getTotalPrice()));
        assertEquals(2, b.getParticipants().size());
    }

    @Test
    void elPlazoDePagoEsDe24Horas() {
        Booking b = service.createBooking(pedido(1, adultos(1)));

        long horas = Duration.between(LocalDateTime.now(), b.getPaymentDeadline()).toMinutes();
        assertTrue(horas >= 23 * 60 + 58 && horas <= 24 * 60, "El plazo debería ser de 24 horas, fue de " + horas + " min");
    }

    @Test
    void siLaActividadEmpiezaAntesElPlazoEsElInicio() {
        LocalDateTime inicio = LocalDateTime.now().plusHours(5);
        instancia.setDateTime(inicio);

        Booking b = service.createBooking(pedido(1, adultos(1)));

        assertEquals(inicio, b.getPaymentDeadline());
    }

    @Test
    void noSePuedeReservarMasLugaresQueLosDisponibles() {
        instancia.setAvailablePlaces(1);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createBooking(pedido(2, adultos(2))));

        assertEquals(409, codigo(e));
        assertEquals(1, instancia.getAvailablePlaces());
    }

    @Test
    void noSePuedeReservarDespuesDelTiempoLimite() {
        instancia.setDateTime(LocalDateTime.now().plusMinutes(30));
        instancia.setMinutesBeforeBookingDeadline(60);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createBooking(pedido(1, adultos(1))));

        assertEquals(409, codigo(e));
    }

    @Test
    void soloLosTuristasPuedenReservar() {
        turista.setRole("PROVIDER");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createBooking(pedido(1, adultos(1))));

        assertEquals(403, codigo(e));
    }

    @Test
    void laCantidadDeParticipantesTieneQueCoincidir() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createBooking(pedido(3, adultos(2))));

        assertEquals(400, codigo(e));
    }

    @Test
    void noSePermitenCedulasRepetidas() {
        List<Participant> repetidos = new ArrayList<Participant>();
        repetidos.add(persona("Ana", "1.234.567-2", LocalDate.of(1990, 1, 1)));
        repetidos.add(persona("Luis", "12345672", LocalDate.of(1992, 2, 2)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createBooking(pedido(2, repetidos)));

        assertEquals(400, codigo(e));
    }

    @Test
    void laCedulaDebeTenerSieteOOchoDigitos() {
        List<Participant> corta = new ArrayList<Participant>();
        corta.add(persona("Ana", "12345", LocalDate.of(1990, 1, 1)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createBooking(pedido(1, corta)));

        assertEquals(400, codigo(e));
    }

    @Test
    void faltanDatosDeUnParticipante() {
        List<Participant> incompleto = new ArrayList<Participant>();
        incompleto.add(persona("", "12345672", LocalDate.of(1990, 1, 1)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createBooking(pedido(1, incompleto)));

        assertEquals(400, codigo(e));
    }

    @Test
    void respetaLaEdadMinimaDeLaActividad() {
        atraccion.setMinimumAge(10);
        List<Participant> menor = new ArrayList<Participant>();
        menor.add(persona("Nico", "12345672", LocalDate.now().minusYears(8)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createBooking(pedido(1, menor)));

        assertEquals(400, codigo(e));
        assertEquals(10, instancia.getAvailablePlaces());
    }

    @Test
    void laCedulaSeGuardaSoloConNumeros() {
        List<Participant> uno = new ArrayList<Participant>();
        uno.add(persona("Ana", "1.234.567-2", LocalDate.of(1990, 1, 1)));

        Booking b = service.createBooking(pedido(1, uno));

        assertEquals("12345672", b.getParticipants().get(0).getDocumentId());
    }

    // ---------- vencimiento de 24 h ----------

    @Test
    void lasReservasVencidasSeCancelanYLiberanLosLugares() {
        Booking vencida = reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);
        instancia.setAvailablePlaces(8);
        List<Booking> lista = new ArrayList<Booking>();
        lista.add(vencida);
        when(bookings.findByStatusAndPaymentDeadlineBefore(eq(Booking.BookingStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(lista);

        service.cancelarReservasVencidas();

        assertEquals(Booking.BookingStatus.CANCELLED, vencida.getStatus());
        assertNotNull(vencida.getCancelReason());
        assertEquals(10, instancia.getAvailablePlaces());
    }

    // ---------- pago ----------

    @Test
    void pagoAprobadoConfirmaLaReserva() {
        Booking b = reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);
        when(pasarela.cobrar(any(), any(), any(), any()))
                .thenReturn(new PaymentGatewayService.Resultado(Payment.APPROVED, "999", "ok", "4242"));

        Booking pagada = service.pay(20L, 7L, tarjeta());

        assertEquals(Booking.BookingStatus.CONFIRMED, pagada.getStatus());
        verify(pagos).save(any(Payment.class));
    }

    @Test
    void elMontoCobradoSaleDeLaReservaNoDelNavegador() {
        reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);
        when(pasarela.cobrar(any(), any(), any(), any()))
                .thenReturn(new PaymentGatewayService.Resultado(Payment.APPROVED, "999", "ok", "4242"));
        Map<String, Object> datos = tarjeta();
        datos.put("transaction_amount", 1);

        service.pay(20L, 7L, datos);

        verify(pasarela).cobrar(eq(new BigDecimal("2000")), any(), any(), any());
    }

    @Test
    void pagoRechazadoDejaLaReservaPendiente() {
        Booking b = reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);
        when(pasarela.cobrar(any(), any(), any(), any()))
                .thenReturn(new PaymentGatewayService.Resultado(Payment.REJECTED, "1", "cc_rejected_insufficient_amount", "0002"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(20L, 7L, tarjeta()));

        assertEquals(402, codigo(e));
        assertEquals(Booking.BookingStatus.PENDING_PAYMENT, b.getStatus());
    }

    @Test
    void pagoEnRevisionNoConfirmaLaReserva() {
        Booking b = reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);
        when(pasarela.cobrar(any(), any(), any(), any()))
                .thenReturn(new PaymentGatewayService.Resultado(Payment.IN_PROCESS, "2", "pending", "0077"));

        Booking resultado = service.pay(20L, 7L, tarjeta());

        assertEquals(Booking.BookingStatus.PENDING_PAYMENT, resultado.getStatus());
    }

    @Test
    void siLaPasarelaNoRespondeLaReservaSigueDisponible() {
        Booking b = reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);
        when(pasarela.cobrar(any(), any(), any(), any()))
                .thenThrow(new PaymentGatewayService.NoDisponibleException("sin conexion"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(20L, 7L, tarjeta()));

        assertEquals(503, codigo(e));
        assertEquals(Booking.BookingStatus.PENDING_PAYMENT, b.getStatus());
    }

    @Test
    void nadieMasPuedePagarUnaReservaAjena() {
        reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(20L, 99L, tarjeta()));

        assertEquals(403, codigo(e));
        verify(pasarela, never()).cobrar(any(), any(), any(), any());
    }

    @Test
    void noSePuedePagarUnaReservaYaVencida() {
        Booking b = reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);
        b.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(20L, 7L, tarjeta()));

        assertEquals(409, codigo(e));
        verify(pasarela, never()).cobrar(any(), any(), any(), any());
    }

    @Test
    void noSePuedePagarDosVecesLaMismaReserva() {
        reservaGuardada(Booking.BookingStatus.CONFIRMED);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(20L, 7L, tarjeta()));

        assertEquals(409, codigo(e));
    }

    @Test
    void sinDatosDeTarjetaNoSeCobra() {
        reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(20L, 7L, new HashMap<String, Object>()));

        assertEquals(400, codigo(e));
        verify(pasarela, never()).cobrar(any(), any(), any(), any());
    }

    // ---------- cancelación ----------

    @Test
    void cancelarUnaReservaConfirmadaReembolsaElPorcentajeYLiberaLugares() {
        Booking b = reservaGuardada(Booking.BookingStatus.CONFIRMED);
        instancia.setAvailablePlaces(8);
        Payment pago = new Payment();
        pago.setStatus(Payment.APPROVED);
        pago.setExternalReference("999");
        List<Payment> lista = new ArrayList<Payment>();
        lista.add(pago);
        when(pagos.findByBookingId(20L)).thenReturn(lista);

        Booking cancelada = service.cancel(20L, 7L);

        assertEquals(Booking.BookingStatus.CANCELLED, cancelada.getStatus());
        assertEquals(0, new BigDecimal("1000.00").compareTo(cancelada.getRefundAmount()));
        assertEquals(10, instancia.getAvailablePlaces());
        verify(pasarela).reembolsar(eq("999"), any(BigDecimal.class));
    }

    @Test
    void siLaActividadNoPermiteCancelarNoSeCancela() {
        reservaGuardada(Booking.BookingStatus.CONFIRMED);
        atraccion.setAllowsCancellation(false);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.cancel(20L, 7L));

        assertEquals(409, codigo(e));
    }

    @Test
    void noSePuedeCancelarFueraDelPlazo() {
        reservaGuardada(Booking.BookingStatus.CONFIRMED);
        instancia.setDateTime(LocalDateTime.now().plusMinutes(30));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.cancel(20L, 7L));

        assertEquals(409, codigo(e));
    }

    @Test
    void cancelarUnaReservaPendienteNoReembolsaNada() {
        Booking b = reservaGuardada(Booking.BookingStatus.PENDING_PAYMENT);
        instancia.setAvailablePlaces(8);

        Booking cancelada = service.cancel(20L, 7L);

        assertEquals(Booking.BookingStatus.CANCELLED, cancelada.getStatus());
        assertEquals(10, instancia.getAvailablePlaces());
        verify(pasarela, never()).reembolsar(any(), any());
    }

    @Test
    void soloElDuenoPuedeCancelar() {
        reservaGuardada(Booking.BookingStatus.CONFIRMED);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.cancel(20L, 99L));

        assertEquals(403, codigo(e));
    }
}
