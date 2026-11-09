package uy.edu.um.proytic2026r.repositorio;

import jakarta.persistence.LockModeType;
import uy.edu.um.proytic2026r.entities.ActivityInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityInstanceRepository extends JpaRepository<ActivityInstance, Long> {

    List<ActivityInstance> findByAtraccionId(Long atraccionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ActivityInstance i where i.id = :id")
    Optional<ActivityInstance> findByIdForUpdate(@Param("id") Long id);
}