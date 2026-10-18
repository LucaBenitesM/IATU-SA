package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Útil para que un turista consulte sus reservas hechas
    List<Booking> findByTouristId(Long touristId);
}