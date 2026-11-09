package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByTouristId(Long touristId);

    List<Booking> findByActivityInstance_Atraccion_Prestador_Owner_Id(Long ownerId);
}