package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {
}