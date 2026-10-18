package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityInstanceRepository extends JpaRepository<ActivityInstance, Long> {
    // Útil para buscar las instancias/horarios de una atracción específica
    List<ActivityInstance> findByAtraccionId(Long atraccionId);
}