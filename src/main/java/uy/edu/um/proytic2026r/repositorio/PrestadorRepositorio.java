package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrestadorRepositorio extends JpaRepository<Prestador, Long> {
    java.util.List<Prestador> findByStatus(String status);
}
