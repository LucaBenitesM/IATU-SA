package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByTouristId(Long touristId);

    List<Booking> findByStatusAndPaymentDeadlineBefore(Booking.BookingStatus status, LocalDateTime limite);

    List<Booking> findByActivityInstance_Atraccion_Prestador_Owner_Id(Long ownerId);
}