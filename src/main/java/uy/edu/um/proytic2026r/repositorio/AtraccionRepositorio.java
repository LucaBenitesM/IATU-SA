package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.Atraccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtraccionRepositorio extends JpaRepository<Atraccion, Long> {

    boolean existsByCategory_Id(Long categoryId);

    boolean existsByProvidedEquipment_Id(Long equipmentId);

    boolean existsByRequiredEquipment_Id(Long equipmentId);
}
