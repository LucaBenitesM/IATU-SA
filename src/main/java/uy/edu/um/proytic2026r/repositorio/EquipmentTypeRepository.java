package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {

    List<EquipmentType> findAllByOrderByNameAsc();

    Optional<EquipmentType> findByNameIgnoreCase(String name);
}
