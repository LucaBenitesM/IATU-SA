package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.Booking;
import uy.edu.um.proytic2026r.servicio.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingControl {

    private final BookingService bookingService;

    public BookingControl(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> crearReserva(@RequestBody Booking booking) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(booking));
    }

    // Lista las reservas de un turista concreto: /api/bookings/tourist/3
    @GetMapping("/tourist/{touristId}")
    public ResponseEntity<List<Booking>> listarPorTurista(@PathVariable Long touristId) {
        return ResponseEntity.ok(bookingService.getBookingsByTourist(touristId));
    }
}
