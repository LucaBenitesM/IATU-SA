package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByAtraccion_IdOrderByCreatedAtDesc(Long atraccionId);

    List<Review> findByTourist_Id(Long touristId);

    List<Review> findByCreatedAtBetween(LocalDateTime desde, LocalDateTime hasta);

    Optional<Review> findByBooking_Id(Long bookingId);

    long countByAtraccion_Id(Long atraccionId);

    @Query("select avg(r.rating) from Review r where r.atraccion.id = :id")
    Double averageByAtraccionId(@Param("id") Long id);

    // Promedio y cantidad de reseñas de todas las atracciones: [idAtraccion, promedio, cantidad]
    @Query("select r.atraccion.id, avg(r.rating), count(r) from Review r group by r.atraccion.id")
    List<Object[]> summaryPerAtraccion();
}
