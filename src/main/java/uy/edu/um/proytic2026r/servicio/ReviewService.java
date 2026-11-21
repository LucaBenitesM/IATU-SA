package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Booking;
import uy.edu.um.proytic2026r.entities.Review;
import uy.edu.um.proytic2026r.repositorio.BookingRepository;
import uy.edu.um.proytic2026r.repositorio.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    // Solo puede valorar el turista dueño de la reserva, con la reserva confirmada
    // y la actividad ya terminada (inicio + duración anterior a este momento).
    @Transactional
    public Review createReview(Long bookingId, Long touristId, Integer rating, String comment) {
        if (bookingId == null || touristId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan datos para valorar.");
        }
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La reserva no existe.");
        }
        if (booking.getTourist() == null || !booking.getTourist().getId().equals(touristId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo quien hizo la reserva puede valorar la actividad.");
        }
        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED
                && booking.getStatus() != Booking.BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo podés valorar actividades de reservas confirmadas.");
        }
        if (!actividadTerminada(booking)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vas a poder valorar la actividad cuando la hayas completado.");
        }
        validar(rating, comment);
        if (reviewRepository.findByBooking_Id(bookingId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya valoraste esta reserva. Podés editar tu reseña.");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setTourist(booking.getTourist());
        review.setAtraccion(booking.getActivityInstance().getAtraccion());
        review.setRating(rating);
        review.setComment(limpiar(comment));
        review.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Transactional
    public Review updateReview(Long reviewId, Long touristId, Integer rating, String comment) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La reseña no existe.");
        }
        if (touristId == null || !review.getTourist().getId().equals(touristId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta reseña no es tuya.");
        }
        validar(rating, comment);
        review.setRating(rating);
        review.setComment(limpiar(comment));
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long touristId) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La reseña no existe.");
        }
        if (touristId == null || !review.getTourist().getId().equals(touristId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta reseña no es tuya.");
        }
        reviewRepository.delete(review);
    }

    public List<Review> getByAtraccion(Long atraccionId) {
        return reviewRepository.findByAtraccion_IdOrderByCreatedAtDesc(atraccionId);
    }

    public List<Review> getByTourist(Long touristId) {
        return reviewRepository.findByTourist_Id(touristId);
    }

    public Map<String, Object> getSummary(Long atraccionId) {
        Map<String, Object> resumen = new HashMap<String, Object>();
        Double promedio = reviewRepository.averageByAtraccionId(atraccionId);
        resumen.put("atraccionId", atraccionId);
        resumen.put("average", promedio == null ? null : redondear(promedio));
        resumen.put("count", reviewRepository.countByAtraccion_Id(atraccionId));
        return resumen;
    }

    public List<Map<String, Object>> getAllSummaries() {
        List<Map<String, Object>> resultado = new ArrayList<Map<String, Object>>();
        List<Object[]> filas = reviewRepository.summaryPerAtraccion();
        for (Object[] fila : filas) {
            Map<String, Object> resumen = new HashMap<String, Object>();
            resumen.put("atraccionId", fila[0]);
            resumen.put("average", redondear(((Number) fila[1]).doubleValue()));
            resumen.put("count", fila[2]);
            resultado.add(resumen);
        }
        return resultado;
    }

    private boolean actividadTerminada(Booking booking) {
        LocalDateTime fin = booking.getActivityInstance().getDateTime();
        Integer minutos = booking.getActivityInstance().getAtraccion().getDurationMinutes();
        if (minutos != null && minutos > 0) {
            fin = fin.plusMinutes(minutos);
        }
        return !fin.isAfter(LocalDateTime.now());
    }

    private void validar(Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La valoración tiene que ser de 1 a 5.");
        }
        if (comment != null && comment.trim().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario no puede superar los 1000 caracteres.");
        }
    }

    private String limpiar(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return null;
        }
        return comment.trim();
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
