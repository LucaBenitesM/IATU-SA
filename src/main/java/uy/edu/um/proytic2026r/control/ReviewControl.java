package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.Review;
import uy.edu.um.proytic2026r.servicio.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewControl {

    public static class ReviewRequest {
        public Long bookingId;
        public Long touristId;
        public Integer rating;
        public String comment;
    }

    private final ReviewService reviewService;

    public ReviewControl(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<Review> crear(@RequestBody ReviewRequest pedido) {
        Review creada = reviewService.createReview(pedido.bookingId, pedido.touristId, pedido.rating, pedido.comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Review> editar(@PathVariable Long id, @RequestBody ReviewRequest pedido) {
        return ResponseEntity.ok(reviewService.updateReview(id, pedido.touristId, pedido.rating, pedido.comment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id, @RequestParam Long touristId) {
        reviewService.deleteReview(id, touristId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attraction/{atraccionId}")
    public ResponseEntity<List<Review>> listarPorAtraccion(@PathVariable Long atraccionId) {
        return ResponseEntity.ok(reviewService.getByAtraccion(atraccionId));
    }

    @GetMapping("/attraction/{atraccionId}/summary")
    public ResponseEntity<Map<String, Object>> resumenDeAtraccion(@PathVariable Long atraccionId) {
        return ResponseEntity.ok(reviewService.getSummary(atraccionId));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<Map<String, Object>>> resumenes() {
        return ResponseEntity.ok(reviewService.getAllSummaries());
    }

    @GetMapping("/tourist/{touristId}")
    public ResponseEntity<List<Review>> listarPorTurista(@PathVariable Long touristId) {
        return ResponseEntity.ok(reviewService.getByTourist(touristId));
    }
}
