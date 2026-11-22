package uy.edu.um.proytic2026r.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Booking;
import uy.edu.um.proytic2026r.entities.Review;
import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.BookingRepository;
import uy.edu.um.proytic2026r.repositorio.ReviewRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Reglas de las reseñas: solo la valora quien completó la actividad.
class ReviewServiceTest {

    private ReviewRepository reviews;
    private BookingRepository bookings;
    private ReviewService service;

    @BeforeEach
    void preparar() {
        reviews = mock(ReviewRepository.class);
        bookings = mock(BookingRepository.class);
        service = new ReviewService(reviews, bookings);
        when(reviews.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Booking reserva(Booking.BookingStatus estado, LocalDateTime inicio) {
        Usuario turista = new Usuario();
        turista.setId(7L);
        Atraccion atraccion = new Atraccion();
        atraccion.setId(1L);
        atraccion.setDurationMinutes(60);
        ActivityInstance instancia = new ActivityInstance();
        instancia.setAtraccion(atraccion);
        instancia.setDateTime(inicio);
        Booking b = new Booking();
        b.setTourist(turista);
        b.setActivityInstance(instancia);
        b.setStatus(estado);
        when(bookings.findById(10L)).thenReturn(Optional.of(b));
        return b;
    }

    private int codigo(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    @Test
    void turistaQueCompletoLaActividadPuedeValorar() {
        reserva(Booking.BookingStatus.CONFIRMED, LocalDateTime.now().minusHours(3));
        when(reviews.findByBooking_Id(10L)).thenReturn(Optional.empty());

        Review r = service.createReview(10L, 7L, 5, "  Excelente  ");

        assertEquals(5, r.getRating());
        assertEquals("Excelente", r.getComment());
        verify(reviews).save(any(Review.class));
    }

    @Test
    void noSePuedeValorarUnaActividadQueTodaviaNoTermino() {
        reserva(Booking.BookingStatus.CONFIRMED, LocalDateTime.now().minusMinutes(20));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, 4, "ok"));

        assertEquals(409, codigo(e));
        verify(reviews, never()).save(any(Review.class));
    }

    @Test
    void noSePuedeValorarUnaActividadFutura() {
        reserva(Booking.BookingStatus.CONFIRMED, LocalDateTime.now().plusDays(2));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, 4, null));

        assertEquals(409, codigo(e));
    }

    @Test
    void otroTuristaNoPuedeValorarLaReserva() {
        reserva(Booking.BookingStatus.CONFIRMED, LocalDateTime.now().minusHours(3));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 99L, 4, null));

        assertEquals(403, codigo(e));
    }

    @Test
    void unaReservaSinPagarNoSePuedeValorar() {
        reserva(Booking.BookingStatus.PENDING_PAYMENT, LocalDateTime.now().minusHours(3));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, 4, null));

        assertEquals(409, codigo(e));
    }

    @Test
    void unaReservaCanceladaNoSePuedeValorar() {
        reserva(Booking.BookingStatus.CANCELLED, LocalDateTime.now().minusHours(3));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, 4, null));

        assertEquals(409, codigo(e));
    }

    @Test
    void laValoracionTieneQueEstarEntreUnoYCinco() {
        reserva(Booking.BookingStatus.CONFIRMED, LocalDateTime.now().minusHours(3));

        assertEquals(400, codigo(assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, 0, null))));
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, 6, null))));
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, null, null))));
    }

    @Test
    void elComentarioNoPuedeSuperarLos1000Caracteres() {
        reserva(Booking.BookingStatus.CONFIRMED, LocalDateTime.now().minusHours(3));
        String largo = "a".repeat(1001);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, 5, largo));

        assertEquals(400, codigo(e));
    }

    @Test
    void noSePuedeValorarDosVecesLaMismaReserva() {
        reserva(Booking.BookingStatus.CONFIRMED, LocalDateTime.now().minusHours(3));
        when(reviews.findByBooking_Id(10L)).thenReturn(Optional.of(new Review()));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createReview(10L, 7L, 5, null));

        assertEquals(409, codigo(e));
    }

    @Test
    void reservaInexistenteDevuelve404() {
        when(bookings.findById(55L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createReview(55L, 7L, 5, null));

        assertEquals(404, codigo(e));
    }

    @Test
    void soloElAutorPuedeEditarSuResena() {
        Usuario autor = new Usuario();
        autor.setId(7L);
        Review r = new Review();
        r.setTourist(autor);
        r.setRating(3);
        when(reviews.findById(1L)).thenReturn(Optional.of(r));

        assertEquals(403, codigo(assertThrows(ResponseStatusException.class,
                () -> service.updateReview(1L, 99L, 5, "x"))));

        Review editada = service.updateReview(1L, 7L, 5, "Mejoró");
        assertEquals(5, editada.getRating());
        assertEquals("Mejoró", editada.getComment());
    }

    @Test
    void elPromedioSeRedondeaAUnDecimal() {
        when(reviews.averageByAtraccionId(1L)).thenReturn(4.26);
        when(reviews.countByAtraccion_Id(1L)).thenReturn(5L);

        Map<String, Object> resumen = service.getSummary(1L);

        assertEquals(4.3, resumen.get("average"));
        assertEquals(5L, resumen.get("count"));
    }

    @Test
    void sinResenasElPromedioEsNulo() {
        when(reviews.averageByAtraccionId(2L)).thenReturn(null);
        when(reviews.countByAtraccion_Id(2L)).thenReturn(0L);

        assertEquals(null, service.getSummary(2L).get("average"));
    }
}
